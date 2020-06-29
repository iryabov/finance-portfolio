package com.github.iryabov.invest.service.impl

import com.github.iryabov.invest.entity.Account
import com.github.iryabov.invest.entity.Dial
import com.github.iryabov.invest.entity.CurrencyPair
import com.github.iryabov.invest.entity.WriteOff
import com.github.iryabov.invest.model.AssetView
import com.github.iryabov.invest.model.AccountForm
import com.github.iryabov.invest.model.AccountView
import com.github.iryabov.invest.model.DialForm
import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.relation.DialType
import com.github.iryabov.invest.repository.*
import com.github.iryabov.invest.service.InvestService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.Integer.min
import java.math.BigDecimal
import java.time.LocalDate

@Service
@Transactional
class InvestServiceImpl(
        val accountRepo: AccountRepository,
        val dialRepo: DialRepository,
        val writeOffRepo: WriteOffRepository,
        val rateRepository: RateRepository,
        val stockQuotesRepo: StockQuotesRepository
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
        if (created.amount != P0)
            addExchangeRate(created.date)
        if (setOf(DialType.SALE, DialType.WITHDRAWALS).contains(created.type))
            writeOffByFifo(created)
        if (setOf(DialType.PURCHASE).contains(created.type))
            writeOffByFifo(created.invert())


        return created.id!!
    }

    override fun deleteDial(accountId: Int, id: Long) {
        dialRepo.deleteById(id)
    }

    override fun deactivateDial(accountId: Int, id: Long) {
        dialRepo.deactivate(id)
    }

    override fun getAccount(accountId: Int): AccountView {
        val assets = ArrayList<AssetView>()
        assets.addAll(dialRepo.findAssets(accountId, Currency.RUB))
        val account = AccountView(assets)
        account.calc()
        account.calcProportion()
        return account
    }

    private fun addExchangeRate(date: LocalDate) {
        val exchange: ExchangeRate by lazy { stockQuotesRepo.findCurrencyByBaseAndDate(Currency.USD, date) }
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

    private fun writeOffByFifo(dial: Dial) {
        val fifo = writeOffRepo.findBalance(dial.accountId, dial.ticker, dial.date).listIterator()
        var needToSell = -1 * dial.quantity
        while (needToSell > 0 && fifo.hasNext()) {
            val balance = fifo.next()
            if (balance.balancedQuantity > 0) {
                val writeOff = WriteOff(dialFrom = balance.dialFrom, dialTo = dial.id!!,
                        quantity = min(needToSell, balance.balancedQuantity))
                writeOffRepo.save(writeOff)
                needToSell -= writeOff.quantity
            }
        }
    }

}

private fun AccountView.calc() {
    assets.forEach { a -> a.calc() }
    totalAmount = assets.sumByBigDecimal { a -> a.amount }
    totalDeposit = assets.sumByBigDecimal { a -> a.deposit }
    totalWithdrawals = assets.sumByBigDecimal { a -> a.withdrawals }
    totalSpent = assets.sumByBigDecimal { a -> a.spent }
    totalReceived = assets.sumByBigDecimal { a -> a.received }
    totalAmountCourse = assets.sumByBigDecimal { a -> a.amountCourse }

    totalProfitCourse = calcIncomePercent(totalAmountCourse, totalAmount)
    totalProfitFix = calcIncomePercent(totalAmount + totalReceived, totalSpent)
    totalProfit = calcIncomePercent(totalAmountCourse + totalReceived, totalSpent)
}

private fun AccountView.calcProportion() {
    assets.forEach { a -> a.calcProportion(totalAmount, totalAmountCourse) }
    assert(assets.sumByBigDecimal { a -> a.proportion } == P100)
    assert(assets.sumByBigDecimal { a -> a.proportionCourse } == P100)
    assert(assets.sumByBigDecimal { a -> a.proportionProfit } == P0)
}

private fun AssetView.calc() {
    amountCourse = if (assetPriceNow != null) BigDecimal(quantity) * assetPriceNow else amount
    profitCourse = calcIncomePercent(amountCourse, amount)
    profitFix = calcIncomePercent(amount + received, spent)
    profit = calcIncomePercent(amountCourse + received, spent)
}

private fun AssetView.calcProportion(totalAmount: BigDecimal, totalAmountCourse: BigDecimal) {
    proportion = amount / totalAmount
    proportionCourse = amountCourse / totalAmountCourse
    proportionProfit = proportionCourse - proportion
}

private fun DialForm.toEntityWith(accountId: Int) = Dial(
        accountId = accountId,
        type = type,
        ticker = ticker,
        date = opened ?: LocalDate.now(),
        currency = currency,
        amount = amount,
        quantity = quantity
)

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
            quantity = amount.intValueExact(),
            amount = BigDecimal(quantity),
            type = type.invert(),
            note = note,
            fee = fee,
            tax = tax)
}

private inline fun <T> Iterable<T>.sumByBigDecimal(selector: (T) -> BigDecimal): BigDecimal {
    var sum: BigDecimal = BigDecimal.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

private fun isCurrency(ticker: String): Boolean {
    return Currency.values().any {  c -> c.name == ticker }
}

private fun currencyOf(ticker: String): Currency? {
    return Currency.values().find {  c -> c.name == ticker }
}
