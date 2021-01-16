package com.github.iryabov.invest.service.impl

import com.github.iryabov.invest.entity.*
import com.github.iryabov.invest.entity.Target
import com.github.iryabov.invest.etl.CurrencyRateLoader
import com.github.iryabov.invest.model.*
import com.github.iryabov.invest.relation.*
import com.github.iryabov.invest.relation.Currency
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
import kotlin.collections.HashMap

@Service
@Transactional(timeout = 30000)
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

    override fun getSecurities(name: String): List<SecurityView> {
        return assetRepo.findAllByNameOrTicker(name, Sort.by("ticker")).map { it.toView() }
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
        return portfolioRepo.findAll().map { it.toView(targetRepo.findAllAssetsViews(it.id!!, currency)) }
    }

    override fun getPortfolio(currency: Currency, portfolioId: Int): PortfolioView {
        val portfolioEntity = portfolioRepo.findById(portfolioId).orElseThrow()
        return portfolioEntity.toView(targetRepo.findAllAssetsViews(portfolioId, currency))
    }

    override fun getPortfolioSummary(portfolioId: Int,
                                     year: Int, month: Int?,
                                     currency: Currency): PortfolioSummaryView {
        val from = LocalDate.of(year, month ?: 1, 1)
        val tMonth = if (month == null || month == 12) 1 else month + 1
        val tYear = if (month == null || month == 12) year + 1 else year
        val till = LocalDate.of(tYear, tMonth, 1)
        val assets = targetRepo.findAllTargetHistoryViews(portfolioId, currency, from, till, if (month == null) "1 year" else "1 month")
        val first = assets.first()
        val last = assets.last()
        return diff(first, last)
    }

    override fun getPortfolioHistory(portfolioId: Int,
                                     period: Period,
                                     currency: Currency): List<TargetHistoryView> {
        val from = period.from.invoke()
        val till = LocalDate.now()
        val assets = targetRepo.findAllTargetHistoryViews(portfolioId, currency, from, till, period.interval)
        return fillChart(assets, from, till, period.step,
                { it.date },
                { d, prev ->
                    TargetHistoryView(
                            date = d,
                            balance = prev?.balance ?: P0,
                            netValue = prev?.netValue ?: P0,
                            marketValue = prev?.marketValue ?: P0,
                            profitValue = prev?.profitValue ?: P0,
                            quantity = prev?.quantity ?: 0)
                },
                ::reduce,
                ::round)
    }

    override fun getPortfolioBenchmark(portfolioId: Int,
                                       period: Period,
                                       currency: Currency,
                                       benchmark: String): List<TargetBenchmarkView> {
        val from = period.from.invoke()
        val till = LocalDate.now()
        val benchmarkHistory = securityHistoryRepo.findAllHistoryByTicker(benchmark, from, till, currency)
        val benchmarkStart = benchmarkHistory.firstOrNull()?.price ?: P0
        val portfolioHistory = targetRepo.findAllTargetHistoryViews(portfolioId, currency, from, till, period.interval)
        val portfolioStart = portfolioHistory.firstOrNull()?.marketProfitPercent ?: P0
        return fillAndMergeChart(portfolioHistory, benchmarkHistory,
                from, till, period.step,
                { it.date }, { it.date },
                ::reduce, ::reduce,
                { date, p, b ->
                    TargetBenchmarkView(
                            date = date,
                            portfolioProfit = p?.marketProfitPercent ?: portfolioStart - portfolioStart,
                            benchmarkProfit = calcProfitPercent(b?.price ?: benchmarkStart, benchmarkStart))
                })

    }

    override fun createPortfolio(form: PortfolioForm): Int {
        val portfolio = portfolioRepo.save(form.toEntity())
        return portfolio.id!!
    }

    override fun deletePortfolio(id: Int) {
        portfolioRepo.deleteById(id)
    }

    override fun getPortfolioForm(id: Int): PortfolioForm {
        val entity = portfolioRepo.findById(id).orElseThrow()
        return entity.toForm()
    }

    override fun updatePortfolio(id: Int, form: PortfolioForm) {
        portfolioRepo.findById(id).orElseThrow()
        portfolioRepo.save(form.toEntity(id))
    }

    override fun addAsset(portfolioId: Int, ticker: String): Int {
        val target = targetRepo.save(Target(ticker = ticker, portfolioId = portfolioId))
        return target.id!!
    }

    override fun addAssets(portfolioId: Int, criteria: SecurityCriteria) {
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

    override fun getTargets(currency: Currency, portfolioId: Int, type: TargetType): List<TargetView> {
        val assets = targetRepo.findAllAssetsViews(portfolioId, currency)
        val proportions = targetRepo.findAllByPortfolioIdAndType(portfolioId, type)
                .filter { it.proportion?.greater(P0) ?: false }
                .groupBy { it.ticker }
                .mapValues { if (it.value.isEmpty()) null else it.value[0].proportion }
        val targets = if (type == TargetType.ASSET) {
            assets.map { TargetView(type = type, ticker = it.assetTicker, name = it.assetName, assets = Collections.singletonList(it)) }
        } else {
            type.enumValues().map { TargetView(type = type, ticker = it, name = it, assets = assets.filter { a -> a.typeOf(type) == it }) }
        }
        targets.forEach {
            it.calcAssets(it.assets)
            it.calcProportion(it.assets)
            it.assets = it.assets.sortedByDescending { a -> a.marketValue }
            it.totalTargetProportion = proportions[it.ticker]
        }
        val totalNetValue = targets.sumByBigDecimal { it.totalNetValue }
        val totalMarketValue = targets.sumByBigDecimal { it.totalMarketValue }
        targets.forEach {
            it.calcProportion(totalNetValue, totalMarketValue)
        }
        return targets.sortedByDescending { it.totalMarketValue }
    }

    override fun getTarget(currency: Currency, portfolioId: Int, ticker: String): AssetView {
        val targets = targetRepo.findAllAssetsViews(portfolioId, currency, ticker)
        if (targets.isEmpty())
            return AssetView(assetTicker = ticker, quantity = 0, netValue = P0)
        val target = targets.first()
        target.calc()
        target.calcProportion(P0, P0)
        return target
    }

    override fun saveTarget(portfolioId: Int, type: TargetType, ticker: String, proportion: Int): Int {
        val targets = targetRepo.findAllByPortfolioIdAndType(portfolioId, type)
        val map = targets.groupBy { it.ticker }.mapValues { it.value[0] }
        val target = map[ticker] ?: Target(portfolioId = portfolioId, type = type, ticker = ticker)
        target.proportion = proportion.toBigDecimal()
        val saved = targetRepo.save(target)
        normalizeProportions(map.mapValues { it.value.proportion ?: P0 }, ticker).forEach {
            val other = map[it.key]
            if (other != null) {
                other.proportion = it.value
                targetRepo.save(other)
            }
        }
        return saved.id!!
    }

    override fun saveTargets(portfolioId: Int, type: TargetType, data: Map<String, Int>): Map<String, Int> {
        val targets = targetRepo.findAllByPortfolioIdAndType(portfolioId, type).groupBy { it.ticker }.mapValues { it.value[0] }
        val locks = ArrayList<String>()
        data.forEach {
            if ((targets[it.key]?.proportion ?: P0).notEq(it.value)) {
                targetRepo.save(Target(
                        id = targets[it.key]?.id,
                        portfolioId = portfolioId,
                        type = type,
                        ticker = it.key,
                        proportion = it.value.toBigDecimal()))
                locks.add(it.key)
            }
        }
        val result = HashMap(data)
        normalizeProportions(data.mapValues { it.value.toBigDecimal() }, *locks.toTypedArray()).forEach {
            val other = targets[it.key]
            if (other != null) {
                other.proportion = it.value
                targetRepo.save(other)
                result[it.key] = it.value.toInt()
            }
        }
        return result
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

    override fun getTargetCandidates(portfolioId: Int, criteria: SecurityCriteria): List<SecurityView> {
        return assetRepo.findAllCandidates(
                portfolioId = portfolioId,
                assetClass = criteria.assetClass,
                sector = criteria.sector,
                country = criteria.country,
                currency = criteria.currency,
                accountId = criteria.accountId).map { it.toView() }
    }

    override fun getAnalytics(type: TargetType, portfolioId: Int, currency: Currency): List<ChartView> {
        val assets = targetRepo.findAllAssetsViews(portfolioId, currency)
        assets.forEach { it.calc() }
        return assets.groupBy { it.typeOf(type) }
                .mapValues { it.value.sumByBigDecimal { v -> v.marketValue } }
                .map { ChartView(name = it.key, value = it.value) }
    }

    override fun getTargetProportions(portfolioId: Int, type: TargetType): List<ChartView> {
        return targetRepo.findAllByPortfolioIdAndType(portfolioId, type).map {
            ChartView(name = it.ticker, value = it.proportion ?: P0)
        }
    }

    override fun getBalancedAssets(portfolioId: Int, currency: Currency): BalancedView {
        val assets = targetRepo.findAllAssetsViews(portfolioId, currency)
        assets.forEach { it.calc() }
        val targets: Map<TargetType, Map<String, Target>> = targetRepo.findAllByPortfolioId(portfolioId)
                .groupBy { it.type }.mapValues { it.value.groupBy { v -> v.ticker }.mapValues { t -> t.value.first() } }
        val assetTargets = targets[TargetType.ASSET] ?: emptyMap()
        val totalTargetProportion = assetTargets.values.sumByBigDecimal { it.proportion ?: P0 }
        val totalMarketValue = assets.sumByBigDecimal { it.marketValue }
        val totalBalancedValue = totalMarketValue + (totalMarketValue * totalTargetProportion / P100)

        val sumClass = (targets[TargetType.CLASS]?.values?.sumByBigDecimal { it.proportion ?: P0 }) ?: P0
        val sumSector = (targets[TargetType.SECTOR]?.values?.sumByBigDecimal { it.proportion ?: P0 }) ?: P0
        val sumCountry = (targets[TargetType.COUNTRY]?.values?.sumByBigDecimal { it.proportion ?: P0 }) ?: P0
        val sumCurrency = (targets[TargetType.CURRENCY]?.values?.sumByBigDecimal { it.proportion ?: P0 }) ?: P0

        val targetsClassDeviationPercent: Map<String, BigDecimal> = calcDeviationPercent(assets, assetTargets, TargetType.CLASS,
                targets[TargetType.CLASS], sumClass, totalBalancedValue)
        val targetsSectorDeviationPercent: Map<String, BigDecimal> = calcDeviationPercent(assets, assetTargets, TargetType.SECTOR,
                targets[TargetType.SECTOR], sumSector, totalBalancedValue)
        val targetsCountryDeviationPercent: Map<String, BigDecimal> = calcDeviationPercent(assets, assetTargets, TargetType.COUNTRY,
                targets[TargetType.COUNTRY], sumCountry, totalBalancedValue)
        val targetsCurrencyDeviationPercent: Map<String, BigDecimal> = calcDeviationPercent(assets, assetTargets, TargetType.CURRENCY,
                targets[TargetType.CURRENCY], sumCurrency, totalBalancedValue)

        val maxClassDeviationPercent = targetsClassDeviationPercent.values.filter { it > P0 }.sumByBigDecimal { it }
        val maxSectorDeviationPercent = targetsSectorDeviationPercent.values.filter { it > P0 }.sumByBigDecimal { it }
        val maxCountryDeviationPercent = targetsCountryDeviationPercent.values.filter { it > P0 }.sumByBigDecimal { it }
        val maxCurrencyDeviationPercent = targetsCurrencyDeviationPercent.values.filter { it > P0 }.sumByBigDecimal { it }

        val result = assets.map { it.toBalancedView() }
        result.forEach { a ->
            a.targetClassDeviationPercent = targetsClassDeviationPercent[a.assetClass?.name ?: OTHER]
                    ?: targetsClassDeviationPercent[OTHER] ?: P0
            a.targetSectorDeviationPercent = targetsSectorDeviationPercent[a.assetSector?.name ?: OTHER]
                    ?: targetsSectorDeviationPercent[OTHER] ?: P0
            a.targetCountryDeviationPercent = targetsCountryDeviationPercent[a.assetCountry?.name ?: OTHER]
                    ?: targetsCountryDeviationPercent[OTHER] ?: P0
            a.targetCurrencyDeviationPercent = targetsCurrencyDeviationPercent[a.assetCurrency?.name ?: OTHER]
                    ?: targetsCurrencyDeviationPercent[OTHER] ?: P0

            a.totalTargetDeviationPercent = (a.targetClassDeviationPercent
                    + a.targetSectorDeviationPercent
                    + a.targetCountryDeviationPercent
                    + a.targetCurrencyDeviationPercent)

            a.proportion = assetTargets[a.ticker]?.proportion ?: P0
            a.balance = (totalMarketValue * a.proportion / P100)
        }

        return BalancedView(
                totalMarketValue = totalMarketValue,
                balance = totalBalancedValue - totalMarketValue,
                deviation = (maxClassDeviationPercent + maxSectorDeviationPercent + maxCountryDeviationPercent + maxCurrencyDeviationPercent).divide(BigDecimal(4)),
                assets = result.sortedByDescending { it.totalTargetDeviationPercent })
    }

    override fun resetAssetTargets(portfolioId: Int) {
        targetRepo.updateProportionByPortfolioIdAndType(portfolioId, TargetType.ASSET)
    }

    override fun plusAssetTarget(portfolioId: Int, ticker: String, amount: BigDecimal) {
        val result = getOrNewTarget(portfolioId, ticker)
        result.proportion = (result.proportion ?: P0).plus(amount)
        targetRepo.save(result)
    }

    override fun minusAssetTarget(portfolioId: Int, ticker: String, amount: BigDecimal) {
        val result = getOrNewTarget(portfolioId, ticker)
        result.proportion = (result.proportion ?: P0).minus(amount)
        targetRepo.save(result)
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

    private fun getOrNewTarget(portfolioId: Int, ticker: String): Target {
        val target = targetRepo.findByPortfolioIdAndTickerAndType(portfolioId, ticker, TargetType.ASSET)
        return if (target.isEmpty)
            Target(portfolioId = portfolioId, ticker = ticker, proportion = P0, type = TargetType.ASSET)
        else
            target.get()
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
    totalNetValue = assets.sumByBigDecimal { a -> a.netValue }
    totalDeposit = assets.sumByBigDecimal { a -> a.deposit }
    totalWithdrawals = assets.sumByBigDecimal { a -> a.withdrawals }
    totalExpenses = assets.sumByBigDecimal { a -> a.expenses }
    totalProceeds = assets.sumByBigDecimal { a -> a.proceeds }
    totalMarketValue = assets.sumByBigDecimal { a -> a.marketValue }

    totalValueProfit = totalMarketValue - totalNetValue
    totalDepositValueProfitPercent = calcProfitPercent(totalMarketValue, totalNetValue).round()
    totalFixedProfit = (totalNetValue + totalProceeds) - totalExpenses
    totalDepositFixedProfitPercent = calcProfitPercent(totalNetValue, totalExpenses - totalProceeds).round()
    totalMarketProfit = (totalMarketValue + totalProceeds) - totalExpenses
    totalDepositMarketProfitPercent = calcProfitPercent(totalMarketValue, totalExpenses - totalProceeds).round()

    totalTargetProportion = assets.sumByBigDecimal { a -> a.targetProportion }
}

private fun ValueView.calcProportion(assets: List<AssetView>) {
    assets.forEach { a -> a.calcProportion(totalNetValue, totalMarketValue) }
    assert(assets.sumByBigDecimal { a -> a.netProportion }.eqOr(P100, P0))
    assert(assets.sumByBigDecimal { a -> a.marketProportion }.eqOr(P100, P0))
    assert(assets.sumByBigDecimal { a -> a.marketProfitProportion }.eqOr(P0, P0))
}

private fun ValueView.calcProportion(totalNetValue: BigDecimal, totalMarketValue: BigDecimal) {
    this.totalNetProportion = calcPercent(this.totalNetValue, totalNetValue).round()
    this.totalMarketProportion = calcPercent(this.totalMarketValue, totalMarketValue).round()
}

private fun AccountView.calcCurrencies() {
    currencies = assets.filter { it.assetTicker.isCurrency() }.sortedByDescending { it.marketValue }
    securities = assets.filter { !it.assetTicker.isCurrency() }.sortedByDescending { it.marketValue }
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
    val targetValue = calcValue(totalMarketValue, targetProportion)
    targetDeviation = if (targetProportion.greater(P0)) marketValue - targetValue else null
    targetDeviationPercent = if (targetProportion.greater(P0)) targetProportion - marketProportion else null
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
    result.quantity = a.quantity + b.quantity
    result.price = maxStrong(a.price, b.price)
    return result
}

private fun needWriteOff(created: Deal) =
        (created.quantity != 0 || created.volume.notZero())
                && setOf(DealType.PURCHASE, DealType.SALE, DealType.WITHDRAWALS, DealType.TAX).contains(created.type)

private fun PortfolioForm.toEntity(id: Int? = null) = Portfolio(
        id = id,
        name = name,
        note = note,
        beginDate = beginDate,
        endDate = endDate
)

private fun Portfolio.toForm() = PortfolioForm(
        id = id,
        name = name,
        note = note,
        beginDate = beginDate,
        endDate = endDate
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
    view.assets = view.assets.sortedByDescending { it.marketValue }
    return view
}

private fun TargetForm.toEntity(id: Int?, portfolioId: Int, ticker: String) = Target(
        id = id,
        portfolioId = portfolioId,
        ticker = ticker,
        proportion = if (targetProportion != null) BigDecimal(targetProportion!!) else null,
        takeProfit = takeProfit,
        stopLoss = stopLoss
)

private fun AssetView.typeOf(type: TargetType): String {
    val other = OTHER
    return when (type) {
        TargetType.ASSET -> this.assetTicker
        TargetType.CLASS -> this.assetClass?.name ?: other
        TargetType.SECTOR -> this.assetSector?.name ?: other
        TargetType.COUNTRY -> this.assetCountry?.name ?: other
        TargetType.CURRENCY -> this.assetCurrency?.name ?: other
    }
}

private fun TargetType.enumValues(): List<String> {
    return when (this) {
        TargetType.ASSET -> Collections.emptyList()
        TargetType.CLASS -> AssetClass.values().map { it.name }
        TargetType.SECTOR -> Sector.values().map { it.name }
        TargetType.COUNTRY -> Country.values().map { it.name }
        TargetType.CURRENCY -> Currency.values().map { it.name }
    }
}

private fun reduce(a: TargetHistoryView, b: TargetHistoryView): TargetHistoryView {
    val result = TargetHistoryView(date = b.date)
    result.balance = avg(a.balance, b.balance)
    result.netValue = avg(a.netValue, b.netValue)
    result.marketValue = avg(a.marketValue, b.marketValue)
    result.profitValue = avg(a.profitValue, b.profitValue)
    return result
}

private fun reduce(a: SecurityHistoryView, b: SecurityHistoryView): SecurityHistoryView {
    return SecurityHistoryView(date = b.date, price = avg(a.price, b.price))
}

private fun round(target: TargetHistoryView): TargetHistoryView {
    target.balance = target.balance.round(0)
    target.netValue = target.netValue.round(0)
    target.profitValue = target.profitValue.round(0)
    target.marketValue = target.marketValue.round(0)
    return target
}

private fun AssetView.toBalancedView(): BalancedAssetView = BalancedAssetView(
        ticker = this.assetTicker,
        name = this.assetName,
        marketValue = this.marketValue,
        netValue = this.netValue,
        assetClass = this.assetClass,
        assetCountry = this.assetCountry,
        assetSector = this.assetSector,
        assetCurrency = this.assetCurrency)

private fun calcDeviationPercent(assets: List<AssetView>,
                                 targets: Map<String, Target>,
                                 type: TargetType,
                                 targetsProportion: Map<String, Target>?,
                                 targetSum: BigDecimal,
                                 totalMarketValue: BigDecimal): Map<String, BigDecimal> {
    val other = P100 - targetSum
    return assets.groupBy {
        val key = it.typeOf(type)
        val target = targetsProportion?.get(key)?.proportion ?: P0
        if (target.greater(P0))
            key
        else
            OTHER
    }.mapValues { a ->
        val targetProportion = if (a.key != OTHER)
            targetsProportion?.get(a.key)?.proportion ?: P0
        else
            other

        val total = a.value.sumByBigDecimal {
            val target = targets[it.assetTicker]
            if (target?.proportion != null)
                it.marketValue + (totalMarketValue * target.proportion!! / P100)
            else
                it.marketValue
        }
        val totalProportion = calcPercent(total, totalMarketValue)
        targetProportion - totalProportion
    }
}

private fun diff(first: TargetHistoryView, last: TargetHistoryView): PortfolioSummaryView {
    return PortfolioSummaryView(
            deposit = P0,
            depositChange = P0,
            withdrawals = P0,
            withdrawalsChange = P0,
            marketValue = last.marketValue,
            marketValueChange = last.marketValue - first.marketValue,
            netValue = last.netValue,
            netValueChange = last.netValue - first.netValue,
            dividends = last.dividends,
            dividendsChange = last.dividends - first.dividends,
            coupons = last.coupons,
            couponsChange = last.coupons - first.coupons,
            percents = last.percents,
            percentsChange = last.percents - first.percents,
            trading = last.trading,
            tradingChange = last.trading - first.trading,
            marketProfit = last.marketValue - last.netValue,
            marketProfitChange = (last.marketValue - last.netValue) - (first.marketValue - first.netValue),
            grossProfit = last.dividends + last.coupons + last.percents + last.trading,
            grossProfitChange = (last.dividends + last.coupons + last.percents + last.trading) - (first.dividends + first.coupons + first.percents + first.trading),
            netProfit = P0,
            netProfitChange = P0,
            tax = last.taxes,
            taxChange = last.taxes - first.taxes,
            fee = P0,
            feeChange = P0
    )
}