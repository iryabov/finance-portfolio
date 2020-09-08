package com.github.iryabov.invest.service.impl

import com.github.iryabov.invest.entity.*
import com.github.iryabov.invest.entity.Target
import com.github.iryabov.invest.etl.CurrencyRateLoader
import com.github.iryabov.invest.model.*
import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.relation.DealType
import com.github.iryabov.invest.relation.Period
import com.github.iryabov.invest.repository.*
import com.github.iryabov.invest.service.InvestService
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.Integer.min
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.*
import kotlin.collections.ArrayList

@Service
@Transactional
class InvestServiceImpl(
        val accountRepo: AccountRepository,
        val dealRepo: DealRepository,
        val writeOffRepo: WriteOffRepository,
        val rateRepository: CurrencyRateRepository,
        val assetRepo: AssetRepository,
        val securityHistoryRepo: SecurityHistoryRepository,
        val remittanceRepository: RemittanceRepository,
        val currencyRateLoader: CurrencyRateLoader,
        val portfolioRepo: PortfolioRepository,
        val targetRepo: TargetRepository
) : InvestService {
    override fun createAccount(form: AccountForm): Int {
        val created = accountRepo.save(form.toEntity())
        return created.id!!
    }

    override fun deleteAccount(id: Int) {
        accountRepo.deleteById(id)
    }

    override fun addDeal(accountId: Int, form: DealForm): Long {
        val created = dealRepo.save(form.toEntityWith(accountId))
        if (created.volume != P0)
            currencyRateLoader.addExchangeRate(created.date)
        writeOffByFifoAndRecalculation(if (created.quantity < 0) created else created.invert(), needWriteOff(created))
        if ((form.type == DealType.WITHDRAWALS || form.type == DealType.DEPOSIT) && form.remittanceAccountId != null) {
            val remittanceDial = dealRepo.save(form.toEntityWith(form.remittanceAccountId!!).invert())
            writeOffByFifoAndRecalculation(if (remittanceDial.quantity < 0) remittanceDial else remittanceDial.invert(), needWriteOff(remittanceDial))
            remittanceRepository.save(Remittance(dialFrom = created.id!!, dialTo = remittanceDial.id!!))
        } else if (setOf(DealType.DIVIDEND, DealType.COUPON, DealType.PERCENT).contains(form.type) && form.remittanceAccountId != null) {
            remittanceDeal(accountId, form.remittanceAccountId!!, form.toRemittanceForm())
        }
        return created.id!!
    }

    override fun remittanceDeal(accountFrom: Int, accountTo: Int, form: RemittanceForm): Pair<Long, Long> {
        val from = dealRepo.save(form.toDealFormWith(DealType.WITHDRAWALS).toEntityWith(accountFrom))
        if (from.volume != P0)
            currencyRateLoader.addExchangeRate(from.date)
        writeOffByFifoAndRecalculation(from, needWriteOff(from))
        val to = dealRepo.save(form.toDealFormWith(DealType.DEPOSIT).toEntityWith(accountTo))
        writeOffByFifoAndRecalculation(to.invert(), needWriteOff(to))
        val remittance = remittanceRepository.save(Remittance(dialFrom = from.id!!, dialTo = to.id!!))
        return remittance.dialFrom to remittance.dialTo
    }

    override fun deleteDeal(accountId: Int, id: Long) {
        val deleted = dealRepo.findById(id).orElseThrow()
        val remittance = remittanceRepository.findOneByDialFromOrTo(deleted.id!!)
        dealRepo.deleteById(id)
        writeOffByFifoAndRecalculation(if (deleted.quantity < 0) deleted else deleted.invert(), false)
        if (remittance != null) {
            val remittanceId = if (remittance.dialFrom == id) remittance.dialTo else remittance.dialFrom
            val remittanceDeal = dealRepo.findById(remittanceId).orElseThrow()
            dealRepo.deleteById(remittanceDeal.id!!)
            writeOffByFifoAndRecalculation(if (remittanceDeal.quantity < 0) remittanceDeal else remittanceDeal.invert(), false)
        }
    }

    override fun deactivateDeal(accountId: Int, id: Long) {
        val deactivated = dealRepo.findById(id).orElseThrow()
        val remittance = remittanceRepository.findOneByDialFromOrTo(deactivated.id!!)
        dealRepo.deactivate(id)
        if (deactivated.active)
            writeOffRepo.deleteAllByDialId(deactivated.id!!)
        writeOffByFifoAndRecalculation(if (deactivated.quantity < 0) deactivated else deactivated.invert(),
                needWriteOff(deactivated) && !deactivated.active)
        if (remittance != null) {
            val remittanceId = if (remittance.dialFrom == id) remittance.dialTo else remittance.dialFrom
            val remittanceDeal = dealRepo.findById(remittanceId).orElseThrow()
            dealRepo.deactivate(remittanceDeal.id!!)
            if (remittanceDeal.active)
                writeOffRepo.deleteAllByDialId(remittanceDeal.id!!)
            writeOffByFifoAndRecalculation(if (remittanceDeal.quantity < 0) remittanceDeal else remittanceDeal.invert(),
                    needWriteOff(remittanceDeal) && !remittanceDeal.active)
        }
    }

    override fun getAccount(accountId: Int, currency: Currency): AccountView {
        val accountEntity = accountRepo.findById(accountId).orElseThrow()
        return accountEntity.toView(dealRepo.findAssets(accountId, currency))
    }

    override fun getAccounts(currency: Currency): TotalView {
        val result = TotalView(accountRepo.findAllByActive().map { it.toView(dealRepo.findAssets(it.id!!, currency)) })
        result.calcTotal(result.accounts)
        return result
    }

    override fun getAsset(accountId: Int, currency: Currency, ticker: String): AssetView {
        val assets = dealRepo.findAssets(accountId, currency, ticker)
        if (assets.isEmpty())
            return AssetView(assetTicker = ticker, quantity = 0, netValue = P0)
        val asset = assets.first()
        asset.calc()
        asset.calcProportion(P0, P0)
        return asset
    }

    override fun getAssetHistory(accountId: Int?, ticker: String, period: Period, currency: Currency): List<AssetHistoryView> {
        val from = period.from.invoke()
        val till = LocalDate.now()
        val assetHistory = dealRepo.findAllByPeriod(accountId, ticker, currency, from, till)
        assetHistory.forEach {
            it.purchase = if (it.quantity > 0) it.quantity else 0
            it.purchasePrice = if (it.quantity > 0) it.price else null
            it.sale = if (it.quantity < 0) it.quantity else 0
            it.salePrice = if (it.quantity < 0) it.price else null
        }
        val securityHistory = securityHistoryRepo.findAllHistoryByTicker(ticker, from, till, currency)
        val resultHistory = ArrayList(assetHistory)
        resultHistory.addAll(securityHistory.map { AssetHistoryView(date = it.date, securityPrice = it.price) })
        return fillChart(resultHistory, from, till, period.step,
                { h -> h.date },
                { d, prev -> AssetHistoryView(date = d, securityPrice = prev?.securityPrice ?: P0) },
                ::reduce)
    }

    override fun getDeals(accountId: Int, currency: Currency, ticker: String?): List<DealView> {
        val dials = dealRepo.findAllByAsset(accountId, currency, ticker)
        val old: MutableList<DealView> = ArrayList()
        dials.asReversed().forEach { it.calcDividend(old) }
        return dials
    }

    override fun getSecurities(): List<SecurityView> {
        return assetRepo.findAll(Sort.by("ticker")).map { it.toView() }
    }

    override fun addSecurity(form: Asset) {
        form.newEntity = true
        assetRepo.save(form)
    }

    override fun editSecurity(form: Asset) {
        val old = assetRepo.findById(form.ticker).orElseThrow()
        assetRepo.save(form)
    }

    override fun deleteSecurity(ticker: String) {
        assetRepo.deleteById(ticker)
    }

    override fun getSecurity(ticker: String): SecurityView {
        val securityEntity = assetRepo.findById(ticker).orElseThrow()
        return securityEntity.toView()
    }

    override fun getSecurity(ticker: String,
                             period: Period,
                             currency: Currency): SecurityView {
        val securityEntity = assetRepo.findById(ticker)
                .orElse(Asset(ticker = ticker, name = ticker))
        val from = period.from()
        val till = LocalDate.now()
        val history = securityHistoryRepo.findAllHistoryByTicker(ticker, from, till, currency)
        val chart = fillChart(history, from, till, period.step,
                { s -> s.date },
                { date, prev -> SecurityHistoryView(date = date, price = prev?.price ?: P0) },
                { a, b -> if (b.price.isZero()) SecurityHistoryView(date = b.date, price = a.price) else b })
        return securityEntity.toView(chart, currency)
    }

    override fun getCurrency(pair1: Currency, pair2: Currency, period: Period): CurrencyView {
        val from = period.from()
        val till = LocalDate.now()
        val history = rateRepository.findAllByPair(pair1, pair2, from, till)
        val chart = fillChart(history.map { CurrencyHistoryView(date = it.date, price = it.price) },
                from, till, period.step,
                { s -> s.date },
                { date, prev -> CurrencyHistoryView(date = date, price = prev?.price ?: P0) })
        return CurrencyView(
                pair1 = pair1,
                pair2 = pair2,
                history = chart
        )
    }

    override fun getPortfolios(currency: Currency): List<PortfolioView> {
        return portfolioRepo.findAll().map { it.toView(targetRepo.findAllViews(it.id!!, currency)) }
    }

    override fun getPortfolio(currency: Currency, portfolioId: Int): PortfolioView {
        val portfolioEntity = portfolioRepo.findById(portfolioId).orElseThrow()
        return portfolioEntity.toView(targetRepo.findAllViews(portfolioId, currency))
    }

    override fun createPortfolio(form: PortfolioForm): Int {
        val portfolio = portfolioRepo.save(form.toEntity())
        return portfolio.id!!
    }

    override fun deletePortfolio(id: Int) {
        portfolioRepo.deleteById(id)
    }

    override fun addTarget(portfolioId: Int, ticker: String) {
        val target = targetRepo.save(Target(ticker = ticker, portfolioId = portfolioId))
    }

    override fun addTargets(portfolioId: Int, criteria: SecurityCriteria) {
        assetRepo.findAllCandidates(
                portfolioId = portfolioId,
                assetClass = criteria.assetClass,
                sector = criteria.sector,
                country = criteria.country,
                currency = criteria.currency,
                accountId = criteria.accountId)
                .map { it.toTarget(portfolioId) }
                .forEach { targetRepo.save(it) }
    }

    override fun deactivateTarget(portfolioId: Int, ticker: String) {
        val deactivated = targetRepo.findByPortfolioIdAndTicker(portfolioId, ticker).orElseThrow()
        deactivated.active = !deactivated.active
        targetRepo.save(deactivated)
    }

    override fun deleteTarget(portfolioId: Int, ticker: String) {
        targetRepo.delete(targetRepo.findByPortfolioIdAndTicker(portfolioId, ticker).orElseThrow())
    }

    override fun updateTarget(portfolioId: Int, ticker: String, form: TargetForm) {
        val target = targetRepo.findByPortfolioIdAndTicker(portfolioId, ticker).orElseThrow()
        targetRepo.save(form.toEntity(target.id!!, portfolioId, ticker))
    }

    override fun getTarget(currency: Currency, portfolioId: Int, ticker: String): AssetView {
        val targets = targetRepo.findAllViews(portfolioId, currency, ticker)
        if (targets.isEmpty())
            return AssetView(assetTicker = ticker, quantity = 0, netValue = P0)
        val target = targets.first()
        target.calc()
        target.calcProportion(P0, P0)
        return target
    }

    override fun getTargetCandidates(portfolioId: Int, criteria: SecurityCriteria): List<SecurityView> {
        return assetRepo.findAllCandidates(
                portfolioId = portfolioId,
                assetClass = criteria.assetClass,
                sector = criteria.sector,
                country = criteria.country,
                currency = criteria.currency,
                accountId = criteria.accountId).map { it.toView() }
    }

    private fun writeOffByFifoAndRecalculation(deal: Deal, calc: Boolean = true) {
        if (deal.quantity >= 0) return
        writeOffRepo.deleteAllLaterThan(deal.accountId, deal.ticker, deal.date, deal.id!!)
        if (calc)
            writeOffByFifo(deal)
        val lateDials = dealRepo.findAllSaleAndPurchaseLaterThan(deal.accountId, deal.ticker, deal.date, deal.id!!)
        for (lateDial in lateDials) {
            writeOffByFifo(if (lateDial.quantity < 0) lateDial else lateDial.invert())
        }
    }

    private fun writeOffByFifo(deal: Deal) {
        val fifo = writeOffRepo.findBalance(deal.accountId, deal.ticker, deal.date, deal.id!!).listIterator()
        var needToSell = -1 * deal.quantity
        while (needToSell > 0 && fifo.hasNext()) {
            val balance = fifo.next()
            if (balance.balancedQuantity > 0) {
                val writeOff = WriteOff(dialFrom = balance.dialFrom, dialTo = deal.id!!,
                        quantity = min(needToSell, balance.balancedQuantity), ticker = deal.ticker)
                writeOffRepo.save(writeOff)
                needToSell -= writeOff.quantity
            }
        }
        if (needToSell > 0)
            throw NotEnoughFundsException("Need to sell $needToSell ${deal.ticker}, but they haven't on ${deal.date}")
    }

}

private fun <T : ValueView> ValueView.calcTotal(items: List<T>) {
    totalDeposit = items.sumByBigDecimal { a -> a.totalDeposit }
    totalWithdrawals = items.sumByBigDecimal { a -> a.totalWithdrawals }
    totalNetValue = items.sumByBigDecimal { a -> a.totalNetValue }
    totalMarketValue = items.sumByBigDecimal { a -> a.totalMarketValue }
    totalFixedProfit = items.sumByBigDecimal { a -> a.totalFixedProfit }
    totalMarketProfit = items.sumByBigDecimal { a -> a.totalMarketProfit }
    totalDepositFixedProfitPercent = calcProfitPercent(totalDeposit + totalFixedProfit, totalDeposit)
    totalDepositMarketProfitPercent = calcProfitPercent(totalDeposit + totalMarketProfit, totalDeposit)
}

private fun ValueView.calcAssets(assets: List<AssetView>) {
    assets.forEach { a -> a.calc() }
    val activeAssets = assets.filter { it.active }
    totalNetValue = activeAssets.sumByBigDecimal { a -> a.netValue }
    totalDeposit = activeAssets.sumByBigDecimal { a -> a.deposit }
    totalWithdrawals = activeAssets.sumByBigDecimal { a -> a.withdrawals }
    totalExpenses = activeAssets.sumByBigDecimal { a -> a.expenses }
    totalProceeds = activeAssets.sumByBigDecimal { a -> a.proceeds }
    totalMarketValue = activeAssets.sumByBigDecimal { a -> a.marketValue }

    totalValueProfit = totalMarketValue - totalNetValue
    totalDepositValueProfitPercent = calcProfitPercent(totalMarketValue, totalNetValue)
    totalFixedProfit = (totalNetValue + totalProceeds) - totalExpenses
    totalDepositFixedProfitPercent = calcProfitPercent(totalDeposit + totalFixedProfit, totalDeposit)
    totalDepositFixedProfitPercent = calcPercent(totalNetValue + (totalProceeds - totalExpenses), totalNetValue)
    totalMarketProfit = (totalMarketValue + totalProceeds) - totalExpenses
    totalDepositMarketProfitPercent = calcProfitPercent(totalDeposit + totalMarketProfit, totalDeposit)
    totalDepositMarketProfitPercent = calcPercent(totalMarketValue + (totalProceeds - totalExpenses), totalNetValue)
}

private fun ValueView.calcProportion(assets: List<AssetView>) {
    if (assets.isEmpty()) return
    assets.forEach { a -> a.calcProportion(totalNetValue, totalMarketValue) }
    assert(assets.sumByBigDecimal { a -> a.netProportion }.eq(P100))
    assert(assets.sumByBigDecimal { a -> a.marketProportion }.eq(P100))
    assert(assets.sumByBigDecimal { a -> a.marketProfitProportion }.eq(P0))
}

private fun AccountView.calcCurrencies() {
    currencies = assets.filter { it.assetTicker.isCurrency() }.sortedByDescending { it.marketProportion }
    securities = assets.filter { !it.assetTicker.isCurrency() }.sortedByDescending { it.marketProportion }
}

private fun AssetView.calc() {
    netValue = netValue.round()
    marketValue = if (assetPriceNow != null) (BigDecimal(quantity) * assetPriceNow!!).round() else netValue
    valueProfitPercent = calcProfitPercent(marketValue, netValue).round()
    fixedProfitPercent = calcProfitPercent(netValue + proceeds, expenses).round()
    marketProfitPercent = calcProfitPercent(marketValue + proceeds, expenses).round()
}

private fun AssetView.calcProportion(totalNetValue: BigDecimal, totalMarketValue: BigDecimal) {
    netProportion = calcPercent(netValue, totalNetValue).round()
    marketProportion = calcPercent(marketValue, totalMarketValue).round()
    marketProfitProportion = marketProportion - netProportion
    if (targetProportion != null) {
        val targetValue = calcValue(totalMarketValue, targetProportion)
        targetDeviation = marketValue - targetValue
        targetDeviationPercent = targetProportion - marketProportion
    }
}

private fun DealForm.toEntityWith(accountId: Int): Deal {
    val quantity: Int = when {
        this.type.quantity -> if (!this.type.income) this.quantity!! else this.quantity!!.negate()
        this.type.currency -> if (!this.type.income) this.volume.toInt() else this.volume.toInt().negate()
        else -> 0
    }
    val ticker: String = if (!this.type.currency) this.ticker else this.currency.name
    return Deal(
            accountId = accountId,
            type = type,
            ticker = ticker,
            date = opened ?: LocalDate.now(),
            currency = currency,
            volume = if (type.income) volume else volume.negate(),
            quantity = quantity)
}

private fun RemittanceForm.toDealFormWith(type: DealType): DealForm {
    return DealForm(
            opened = this.opened,
            type = type,
            ticker = this.currency.name,
            currency = this.currency,
            volume = BigDecimal(this.quantity)
    )
}

private fun DealForm.toRemittanceForm(): RemittanceForm {
    return RemittanceForm(
            opened = this.opened!!,
            currency = this.currency,
            quantity = this.volume.toInt()
    )
}

private fun AccountForm.toEntity() = Account(
        name = name,
        num = num
)

private fun Deal.invert(): Deal {
    return Deal(
            id = id,
            active = active,
            date = date,
            accountId = accountId,
            ticker = currency!!.name,
            currency = currencyOf(ticker),
            quantity = volume.round(0).intValueExact(),
            volume = BigDecimal(quantity),
            type = type.invert(),
            note = note,
            fee = fee,
            tax = tax)
}

private fun DealView.calcDividend(old: MutableList<DealView>) {
    if (!this.active)
        return
    if (type == DealType.DIVIDEND && (dividendQuantity ?: 0) > 0) {
        val dividendPerAsset = (volume ?: P0).divide((dividendQuantity ?: 0).toBigDecimal(), 2, RoundingMode.HALF_UP)
        var remainQuantity = dividendQuantity!!
        old.filter { it.assetTicker == this.assetTicker }.forEach {
            remainQuantity -= it.quantity!!
            val quantityByDividendDate = if (remainQuantity < 0) {
                it.quantity!! + remainQuantity
            } else {
                it.quantity!!
            }
            if (quantityByDividendDate > 0)
                it.dividendProfit += (quantityByDividendDate.toBigDecimal() * dividendPerAsset).round(0)
        }
    }
    if ((quantity ?: 0) > 0)
        old.add(0, this)
}

private fun Asset.toView(securityHistory: List<SecurityHistoryView> = Collections.emptyList(),
                         currency: Currency = Currency.RUB): SecurityView {
    val securityView = SecurityView(
            ticker = this.ticker,
            name = this.name,
            assetClass = this.assetClass,
            assetSector = this.sector,
            assetCountry = this.country,
            currency = this.currency ?: currency,
            api = this.api,
            priceNow = this.priceNow ?: P0,
            priceWeek = this.priceWeek ?: P0,
            priceMonth = this.priceMonth ?: P0)
    securityView.history = securityHistory
    return securityView
}

private fun Asset.toTarget(portfolioId: Int): Target = Target(
        portfolioId = portfolioId,
        ticker = ticker,
        active = true)

private fun reduce(a: AssetHistoryView, b: AssetHistoryView): AssetHistoryView {
    val result = AssetHistoryView(b.date)
    result.securityPrice = max(a.securityPrice, b.securityPrice)
    result.purchase = b.purchase + a.purchase
    result.sale = b.sale + a.sale
    result.quantity = a.quantity + b.quantity
    result.purchasePrice = max(a.purchasePrice, b.purchasePrice)
    result.salePrice = max(a.salePrice, b.salePrice)
    return result
}

private fun needWriteOff(created: Deal) =
        (created.quantity != 0 || created.volume.notZero())
                && setOf(DealType.PURCHASE, DealType.SALE, DealType.WITHDRAWALS, DealType.TAX).contains(created.type)

private fun PortfolioForm.toEntity() = Portfolio(
        name = name,
        note = note
)

private fun Account.toView(assets: List<AssetView>): AccountView {
    val view = AccountView(id!!, name, assets)
    view.calcAssets(view.assets)
    view.calcProportion(view.assets)
    view.calcCurrencies()
    return view
}

private fun Portfolio.toView(assets: List<AssetView>): PortfolioView {
    val view = PortfolioView(id!!, name)
    view.assets = assets
    view.calcAssets(view.assets)
    view.calcProportion(view.assets)
    view.assets = view.assets.sortedByDescending { it.marketProportion }
    return view
}

private fun TargetForm.toEntity(id: Long?, portfolioId: Int, ticker: String) = Target(
        id = id,
        portfolioId = portfolioId,
        ticker = ticker,
        proportion = if (targetProportion != null) BigDecimal(targetProportion!!) else null,
        takeProfit = takeProfit,
        stopLoss = stopLoss
)