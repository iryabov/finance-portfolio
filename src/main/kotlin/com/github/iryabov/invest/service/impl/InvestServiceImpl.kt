package com.github.iryabov.invest.service.impl

import com.github.iryabov.invest.client.CurrenciesClient
import com.github.iryabov.invest.client.ExchangeRate
import com.github.iryabov.invest.entity.*
import com.github.iryabov.invest.model.*
import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.relation.DialType
import com.github.iryabov.invest.relation.Period
import com.github.iryabov.invest.repository.*
import com.github.iryabov.invest.service.InvestService
import org.springframework.beans.factory.annotation.Qualifier
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
        val dialRepo: DialRepository,
        val writeOffRepo: WriteOffRepository,
        val rateRepository: CurrencyRateRepository,
        @Qualifier("currenciesClientCBRF")
        val currenciesClient: CurrenciesClient,
        val assetRepo: AssetRepository,
        val securityHistoryRepo: SecurityHistoryRepository
) : InvestService {
    override fun createAccount(form: AccountForm): Int {
        val created = accountRepo.save(form.toEntity())
        return created.id!!
    }

    override fun deleteAccount(id: Int) {
        accountRepo.deleteById(id)
    }

    override fun addDial(accountId: Int, form: DialForm): Long {
        val created = dialRepo.save(form.toEntityWith(accountId))
        if (created.volume != P0)
            addExchangeRate(created.date)
        writeOffByFifoAndRecalculation(if (created.quantity < 0) created else created.invert(), needWriteOff(created))
        return created.id!!
    }

    override fun deleteDial(accountId: Int, id: Long) {
        val deleted = dialRepo.findById(id).orElseThrow()
        dialRepo.deleteById(id)
        writeOffByFifoAndRecalculation(if (deleted.quantity < 0) deleted else deleted.invert(), false)
    }

    override fun deactivateDial(accountId: Int, id: Long) {
        val deactivated = dialRepo.findById(id).orElseThrow()
        dialRepo.deactivate(id)
        if (deactivated.active)
            writeOffRepo.deleteAllByDialId(deactivated.id!!)
        writeOffByFifoAndRecalculation(if (deactivated.quantity < 0) deactivated else deactivated.invert(),
                needWriteOff(deactivated) && !deactivated.active)
    }

    override fun getAccount(accountId: Int, currency: Currency): AccountView {
        val assets = ArrayList<AssetView>()
        val accountEntity = accountRepo.findById(accountId).orElseThrow()
        assets.addAll(dialRepo.findAssets(accountId, currency))
        val account = AccountView(accountId, accountEntity.name, assets)
        account.calc()
        account.calcProportion()
        return account
    }

    override fun getAccounts(): List<AccountView> {
        return accountRepo.findAllByActive().map { getAccount(it.id!!) }
    }

    override fun getAsset(accountId: Int, currency: Currency, ticker: String): AssetView {
        val assets = dialRepo.findAssets(accountId, currency, ticker)
        if (assets.isEmpty())
            return AssetView(assetTicker = ticker, quantity = 0, netValue = P0)
        val asset = assets.first()
        asset.calc()
        asset.calcProportion(P0, P0)
        return asset
    }

    override fun getAssetHistory(accountId: Int, ticker: String, period: Period, currency: Currency): List<AssetHistoryView> {
        val from = period.from.invoke()
        val till = LocalDate.now()
        val assetHistory = dialRepo.findAllByPeriod(accountId, ticker, currency, from, till)
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

    override fun getDials(accountId: Int, ticker: String, currency: Currency): List<DialView> {
        val dials = dialRepo.findAllByAsset(accountId, ticker, currency)
        val old: MutableList<DialView> = ArrayList()
        dials.asReversed().forEach { it.calcDividend(old) }
        return dials
    }

    override fun getSecurities(): List<SecurityView> {
        return assetRepo.findAll().map { it.toView() }
    }

    override fun addSecurity(form: Asset) {
        form.newEntity = true
        assetRepo.save(form)
    }

    override fun editSecurity(form: Asset) {
        val old = assetRepo.findById(form.ticker).orElseThrow()
        assetRepo.save(form)
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
                { date, prev -> SecurityHistoryView(date = date, price = prev?.price ?: P0) })
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

    private fun addExchangeRate(date: LocalDate) {
        val exchange: ExchangeRate by lazy { currenciesClient.findCurrencyByDate(date) }
        for (currencyPurchased in Currency.values()) {
            val rates = rateRepository.findByDateAndBase(date, currencyPurchased)
            for (currencySale in Currency.values().filter { it != currencyPurchased }) {
                if (rates.all { it.currencySale != currencySale }) {
                    rateRepository.save(CurrencyPair(
                            date = date,
                            currencyPurchased = currencyPurchased,
                            currencySale = currencySale,
                            price = exchange.getPairExchangePrice(currencyPurchased, currencySale)))
                }
            }
        }
    }

    private fun writeOffByFifoAndRecalculation(dial: Dial, calc: Boolean = true) {
        if (dial.quantity >= 0) return
        writeOffRepo.deleteAllLaterThan(dial.accountId, dial.ticker, dial.date, dial.id!!)
        if (calc)
            writeOffByFifo(dial)
        val lateDials = dialRepo.findAllSaleAndPurchaseLaterThan(dial.accountId, dial.ticker, dial.date, dial.id!!)
        for (lateDial in lateDials) {
            writeOffByFifo(if (lateDial.quantity < 0) lateDial else lateDial.invert())
        }
    }

    private fun writeOffByFifo(dial: Dial) {
        val fifo = writeOffRepo.findBalance(dial.accountId, dial.ticker, dial.date, dial.id!!).listIterator()
        var needToSell = -1 * dial.quantity
        while (needToSell > 0 && fifo.hasNext()) {
            val balance = fifo.next()
            if (balance.balancedQuantity > 0) {
                val writeOff = WriteOff(dialFrom = balance.dialFrom, dialTo = dial.id!!,
                        quantity = min(needToSell, balance.balancedQuantity), ticker = dial.ticker)
                writeOffRepo.save(writeOff)
                needToSell -= writeOff.quantity
            }
        }
        if (needToSell > 0)
            throw NotEnoughFundsException("Need to sell $needToSell ${dial.ticker}, but they haven't on ${dial.date}")
    }

}

private fun AccountView.calc() {
    assets.forEach { a -> a.calc() }
    totalNetValue = assets.sumByBigDecimal { a -> a.netValue }
    totalDeposit = assets.sumByBigDecimal { a -> a.deposit }
    totalWithdrawals = assets.sumByBigDecimal { a -> a.withdrawals }
    totalExpenses = assets.sumByBigDecimal { a -> a.expenses }
    totalProceeds = assets.sumByBigDecimal { a -> a.proceeds }
    totalMarketValue = assets.sumByBigDecimal { a -> a.marketValue }

    totalValueProfit = totalMarketValue - totalNetValue
    totalValueProfitPercent = calcProfitPercent(totalMarketValue, totalNetValue)
    totalFixedProfit = (totalNetValue + totalProceeds) - totalExpenses
    totalFixedTurnoverProfitPercent = calcProfitPercent(totalNetValue + totalProceeds, totalExpenses)
    totalFixedProfitPercent = calcPercent(totalNetValue + (totalProceeds - totalExpenses), totalNetValue)
    totalMarketProfit = (totalMarketValue + totalProceeds) - totalExpenses
    totalMarketTurnoverProfitPercent = calcProfitPercent(totalMarketValue + totalProceeds, totalExpenses)
    totalMarketProfitPercent = calcPercent(totalMarketValue + (totalProceeds - totalExpenses), totalNetValue)
}

private fun AccountView.calcProportion() {
    assets.forEach { a -> a.calcProportion(totalNetValue, totalMarketValue) }
    assert(assets.sumByBigDecimal { a -> a.netInterest }.eq(P100))
    assert(assets.sumByBigDecimal { a -> a.marketInterest }.eq(P100))
    assert(assets.sumByBigDecimal { a -> a.profitInterest }.eq(P0))
}

private fun AssetView.calc() {
    netValue = netValue.round()
    marketValue = if (assetPriceNow != null) (BigDecimal(quantity) * assetPriceNow!!).round() else netValue
    valueProfit = calcProfitPercent(marketValue, netValue).round()
    fixedProfit = calcProfitPercent(netValue + proceeds, expenses).round()
    marketProfit = calcProfitPercent(marketValue + proceeds, expenses).round()
}

private fun AssetView.calcProportion(totalNetValue: BigDecimal, totalMarketValue: BigDecimal) {
    netInterest = calcPercent(netValue, totalNetValue).round()
    marketInterest = calcPercent(marketValue, totalMarketValue).round()
    profitInterest = marketInterest - netInterest
}

private fun DialForm.toEntityWith(accountId: Int): Dial {
    val quantity: Int = when {
        this.type.quantity -> if (!this.type.income) this.quantity!! else this.quantity!!.negate()
        this.type.currency -> if (!this.type.income) this.volume.toInt() else this.volume.toInt().negate()
        else -> 0
    }
    val ticker: String = if (!this.type.currency) this.ticker else this.currency.name
    return Dial(
            accountId = accountId,
            type = type,
            ticker = ticker,
            date = opened ?: LocalDate.now(),
            currency = currency,
            volume = if (type.income) volume else volume.negate(),
            quantity = quantity)
}

private fun AccountForm.toEntity() = Account(
        name = name,
        num = num
)

private fun Dial.invert(): Dial {
    return Dial(
            id = id,
            active = active,
            date = date,
            accountId = accountId,
            ticker = currency!!.name,
            currency = currencyOf(ticker),
            quantity = volume.intValueExact(),
            volume = BigDecimal(quantity),
            type = type.invert(),
            note = note,
            fee = fee,
            tax = tax)
}

private fun DialView.calcDividend(old : MutableList<DialView>) {
    if (!this.active)
        return
    if (type == DialType.DIVIDEND && (dividendQuantity ?: 0) > 0) {
        val dividendPerAsset = (volume ?: P0).divide((dividendQuantity ?: 0).toBigDecimal(), 2, RoundingMode.HALF_UP)
        var remainQuantity = dividendQuantity!!
        old.forEach {
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

private fun String.isCurrency(): Boolean {
    return Currency.values().any { c -> c.name == this }
}

private fun currencyOf(ticker: String): Currency? {
    return Currency.values().find { c -> c.name == ticker }
}

private fun Asset.toView(securityHistory: List<SecurityHistoryView> = Collections.emptyList(),
                         currency: Currency = Currency.RUB): SecurityView {
    return SecurityView(
            ticker = this.ticker,
            name = this.name,
            assetClass = this.assetClass,
            assetSector = this.sector,
            assetCountry = this.country,
            currency = this.currency ?: currency,
            priceNow = this.priceNow ?: P0,
            priceWeek = this.priceWeek ?: P0,
            priceMonth = this.priceMonth ?: P0,
            history = securityHistory
    )
}

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

private fun needWriteOff(created: Dial) =
        (created.quantity != 0 || created.volume.notZero())
                && setOf(DialType.PURCHASE, DialType.SALE, DialType.WITHDRAWALS, DialType.TAX).contains(created.type)
