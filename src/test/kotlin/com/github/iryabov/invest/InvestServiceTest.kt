package com.github.iryabov.invest

import com.github.iryabov.invest.etl.AssetHistoryLoader
import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.model.AccountForm
import com.github.iryabov.invest.model.DialForm
import com.github.iryabov.invest.relation.DialType
import com.github.iryabov.invest.repository.AccountRepository
import com.github.iryabov.invest.repository.DialRepository
import com.github.iryabov.invest.repository.CurrencyRateRepository
import com.github.iryabov.invest.client.impl.CurrenciesClientCBRF
import com.github.iryabov.invest.client.impl.CurrenciesClientECB
import com.github.iryabov.invest.client.impl.SecuritiesClientMoex
import com.github.iryabov.invest.etl.CurrencyRateLoader
import com.github.iryabov.invest.service.InvestService
import com.github.iryabov.invest.etl.DialsCsvLoader
import com.github.iryabov.invest.model.AssetView
import com.github.iryabov.invest.model.DialView
import com.github.iryabov.invest.relation.Currency.RUB
import com.github.iryabov.invest.relation.DialType.*
import com.github.iryabov.invest.relation.Period
import com.github.iryabov.invest.service.impl.NotEnoughFundsException
import com.github.iryabov.invest.service.impl.date
import com.github.iryabov.invest.service.impl.eq
import com.github.iryabov.invest.service.impl.money
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.jdbc.Sql
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.Month
import java.util.function.Predicate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = ["spring.datasource.url=jdbc:postgresql://localhost:5432/invest_test"])
@Sql("/schema.sql")
class InvestServiceTest(
        @Autowired
        val dialRepo: DialRepository,
        @Autowired
        val accountRepo: AccountRepository,
        @Autowired
        val investService: InvestService,
        @Autowired
        val csvLoader: DialsCsvLoader,
        @Autowired
        val currenciesClientECB: CurrenciesClientECB,
        @Autowired
        val currenciesClientCBRF: CurrenciesClientCBRF,
        @Autowired
        val rateRepo: CurrencyRateRepository,
        @Autowired
        val securitiesClientMoex: SecuritiesClientMoex,
        @Autowired
        val assetHistoryLoader: AssetHistoryLoader,
        @Autowired
        val currencyRateLoader: CurrencyRateLoader
) {

    @BeforeEach
    internal fun setUp() {
        accountRepo.deleteAll()
//        rateRepo.deleteAll()
    }

    @Test
    @Disabled
    fun test() {
        //account creating
        val accountId = investService.createAccount(AccountForm("MyBrocker", "12345"))
        assertThat(accountRepo.findById(accountId)).isPresent

        //dial deposit
        val deposit = investService.addDial(accountId, DialForm(type = DEPOSIT,
                ticker = "RUB",
                volume = BigDecimal(100),
                currency = RUB))
        assertThat(dialRepo.findById(deposit).get()).matches { d ->
            d.ticker == "RUB" && d.accountId == accountId && d.active
                    && d.quantity == 100 && d.volume == BigDecimal(-100)
        }

        //dial purchase
        val purchase = investService.addDial(accountId, DialForm(type = PURCHASE,
                ticker = "TEST",
                volume = BigDecimal(100),
                currency = RUB,
                quantity = 10))
        assertThat(dialRepo.findById(purchase).get()).matches { d ->
            d.ticker == "TEST" && d.accountId == accountId && d.active
                    && d.quantity == 10 && d.volume == BigDecimal(-100)
        }

        var account = investService.getAccount(accountId)
        assertThat(account).matches {
            it.totalNetValue.eq(BigDecimal(100))
            it.assets.find { a -> a.assetTicker == "TEST" }!!.netValue.eq(BigDecimal(100))
        }

        val sale = investService.addDial(accountId, DialForm(type = SALE,
                ticker = "TEST",
                volume = BigDecimal(25),
                currency = RUB,
                quantity = 2))
        assertThat(dialRepo.findById(sale).get()).matches { d ->
            d.ticker == "TEST" && d.accountId == accountId && d.active
                    && d.quantity == -2 && d.volume == BigDecimal(25)
        }

        account = investService.getAccount(accountId)
        assertThat(account).matches {
            it.totalNetValue.eq(BigDecimal(105)) &&
                    it.assets.find { a -> a.assetTicker == "TEST" }!!.netValue.eq(BigDecimal(80))
        }

        //dial deactivating
        investService.deactivateDial(accountId, sale)
        assertThat(dialRepo.findById(sale).get()).matches { d -> !d.active }

        account = investService.getAccount(accountId)
        assertThat(account).matches {
            it.totalNetValue.eq(BigDecimal(100)) &&
                    it.assets.find { a -> a.assetTicker == "TEST" }!!.netValue.eq(BigDecimal(100))
        }

        //dial activating
        investService.deactivateDial(accountId, sale)
        assertThat(dialRepo.findById(sale).get()).matches { d -> d.active }

        account = investService.getAccount(accountId)
        assertThat(account).matches {
            it.totalNetValue.eq(BigDecimal(105)) &&
                    it.assets.find { a -> a.assetTicker == "TEST" }!!.netValue.eq(BigDecimal(80))
        }

        //dial deleting
        investService.deleteDial(accountId, purchase)
        assertThat(dialRepo.findById(purchase).isEmpty).isTrue()

        //account deleting
        investService.deleteAccount(accountId)
        assertThat(accountRepo.findById(accountId).isEmpty).isTrue()
    }

    @Test
//    @Disabled
    fun writeoff() {
        //account creating
        val accountId = investService.createAccount(AccountForm("WriteOffTest", "12345"))
        assertThat(accountRepo.findById(accountId)).isPresent

        assertThatThrownBy {
            investService.addDial(accountId, DialForm("AAA", date("2020-04-07"), PURCHASE, RUB, money(500), 5))
        }.isInstanceOf(NotEnoughFundsException::class.java)

        val deposit2000 = investService.addDial(accountId, DialForm("RUB", date("2020-02-09"),
                DEPOSIT, RUB, money(2000)))

        val purchase5For500 = investService.addDial(accountId, DialForm("AAA", date("2020-04-07"),
                PURCHASE, RUB, money(500), 5))

        val purchase10For1000 = investService.addDial(accountId, DialForm("AAA", date("2020-05-01"),
                PURCHASE, RUB, money(1000), 10))

        val sale5For500 = investService.addDial(accountId, DialForm("AAA", date("2020-06-01"),
                SALE, RUB, money(500), 5))

        val purchase10For950 = investService.addDial(accountId, DialForm("AAA", date("2020-06-22"),
                PURCHASE, RUB, money(950), 10))

        val sale5For450 = investService.addDial(accountId, DialForm("AAA", date("2020-07-07"),
                SALE, RUB, money(450), 5))

        assertThatAsset(accountId, "AAA") { it.quantity == 15 && it.netValue.eq(money(1450)) }
        assertThatDial(purchase5For500) { it.soldQuantity == 5 }
        assertThatDial(purchase10For1000) { it.soldQuantity == 5 }
        assertThatDial(purchase10For950) { it.soldQuantity == null }
        assertThatDial(sale5For450) { it.profit!!.eq(money(-50)) }

        investService.deactivateDial(accountId, purchase10For1000)
        assertThatAsset(accountId, "AAA") { it.quantity == 5 && it.netValue.eq(money(475)) }
        assertThatDial(purchase5For500) { it.soldQuantity == 5 }
        assertThatDial(purchase10For1000) { it.soldQuantity == null }
        assertThatDial(purchase10For950) { it.soldQuantity == 5 }
        assertThatDial(sale5For450) { it.profit!!.eq(money(-25)) }

        val dividend25 = investService.addDial(accountId, DialForm("AAA", date("2020-07-30"),
                DIVIDEND, RUB, money(25)))
        assertThatDial(purchase10For950) { it.dividendProfit.eq(money(25)) }

        investService.deactivateDial(accountId, purchase10For1000)
        assertThatAsset(accountId, "AAA") { it.quantity == 15 && it.netValue.eq(money(1450)) }
        assertThatDial(purchase5For500) { it.soldQuantity == 5 }
        assertThatDial(purchase10For1000) { it.soldQuantity == 5 }
        assertThatDial(purchase10For1000) { it.dividendProfit.eq(money(8)) }
        assertThatDial(purchase10For950) { it.soldQuantity == null }
        assertThatDial(purchase10For950) { it.dividendProfit.eq(money(17)) }
        assertThatDial(sale5For450) { it.profit!!.eq(money(-50)) }

        val dividend20 = investService.addDial(accountId, DialForm("AAA", date("2020-08-20"),
                DIVIDEND, RUB, money(20)))
        assertThatDial(purchase10For1000) { it.dividendProfit.eq(money(15)) }
        assertThatDial(purchase10For950) { it.dividendProfit.eq(money(30)) }

        val sale5For600 = investService.addDial(accountId, DialForm("AAA", date("2020-08-08"),
                SALE, RUB, money(600), 5))
        assertThatAsset(accountId, "AAA") { it.quantity == 10 && it.netValue.eq(money(950)) }
        assertThatDial(purchase10For1000) { it.dividendProfit.eq(money(8)) }
        assertThatDial(purchase10For950) { it.dividendProfit.eq(money(37)) }
        assertThatDial(sale5For600) { it.profit!!.eq(money(100)) }
        assertThatDial(purchase10For1000) { it.soldQuantity == 10 }

        val sale10For1000 = investService.addDial(accountId, DialForm("AAA", date("2020-09-10"),
                SALE, RUB, money(1000), 10))
        assertThatAsset(accountId, "AAA") { it.quantity == 0 && it.netValue.eq(money(0)) }
        assertThatDial(sale10For1000) { it.profit!!.eq(money(50)) }

        val withdrawals = investService.addDial(accountId, DialForm("RUB", date("2020-10-01"),
                WITHDRAWALS, RUB, money(2050)))
        assertThatAsset(accountId, "RUB") { it.quantity == 95 && it.netValue.eq(money(95)) }

        val tax10 = investService.addDial(accountId, DialForm("AAA", date("2021-01-10"),
                TAX, RUB, money(10)))
        assertThatAsset(accountId, "RUB") { it.quantity == 85 && it.netValue.eq(money(85)) }

        //account deleting
        investService.deleteAccount(accountId)
        assertThat(accountRepo.findById(accountId).isEmpty).isTrue()
    }

    @Test()
    @Disabled
    fun getAccountView() {
        csvLoader.load(ClassPathResource("/csv/test.csv"))
        val bank = investService.getAccount(accountRepo.findByName("Bank")?.id!!)
        assertThat(bank.assets.size).isEqualTo(4)
        assertThat(bank.totalDeposit.eq(BigDecimal("101000"))).isTrue()
        assertThat(bank.totalWithdrawals.eq(BigDecimal("82132"))).isTrue()
        assertThat(bank.totalNetValue.eq(BigDecimal("29777"))).isTrue()
        assertThat(bank.totalFixedProfitPercent.eq(BigDecimal("3.2"), 1)).isTrue()

        val broker = investService.getAccount(accountRepo.findByName("Broker")!!.id!!)
        assertThat(broker.assets.size).isEqualTo(5)
        assertThat(broker.totalDeposit.eq(BigDecimal("82132"))).isTrue()
        assertThat(broker.totalWithdrawals.eq(BigDecimal("1000"))).isTrue()
        assertThat(broker.totalNetValue.eq(BigDecimal("89009"))).isTrue()
        assertThat(broker.totalFixedProfitPercent.eq(BigDecimal("4.8"), 1)).isTrue()
    }


    @Test
    @Disabled
    fun exchangeECB() {
        val exchange = currenciesClientECB.findCurrencyByDate(LocalDate.of(2020, Month.JANUARY, 1))
        assertThat(exchange.getPairExchangePrice(Currency.USD, RUB).setScale(3, RoundingMode.HALF_DOWN))
                .isEqualTo(BigDecimal(62.272).setScale(3, RoundingMode.HALF_DOWN))
        assertThat(exchange.getPairExchangePrice(RUB, Currency.USD).setScale(3, RoundingMode.HALF_DOWN))
                .isEqualTo(BigDecimal(0.016).setScale(3, RoundingMode.HALF_DOWN))
        assertThat(exchange.getPairExchangePrice(Currency.EUR, Currency.USD).setScale(3, RoundingMode.HALF_DOWN))
                .isEqualTo(BigDecimal(1.123).setScale(3, RoundingMode.HALF_DOWN))
    }

    @Test
    @Disabled
    fun exchangeCBRF() {
        val exchange = currenciesClientCBRF.findCurrencyByDate(LocalDate.of(2020, Month.JANUARY, 1))
        assertThat(exchange.getPairExchangePrice(Currency.USD, RUB).setScale(3, RoundingMode.HALF_DOWN))
                .isEqualTo(BigDecimal(61.906).setScale(3, RoundingMode.HALF_DOWN))
        assertThat(exchange.getPairExchangePrice(RUB, Currency.USD).setScale(3, RoundingMode.HALF_DOWN))
                .isEqualTo(BigDecimal(0.016).setScale(3, RoundingMode.HALF_DOWN))
        assertThat(exchange.getPairExchangePrice(Currency.EUR, Currency.USD).setScale(3, RoundingMode.HALF_DOWN))
                .isEqualTo(BigDecimal(1.121).setScale(3, RoundingMode.HALF_DOWN))
    }

    @Test
    @Disabled
    fun securitiesClient() {
        val price = securitiesClientMoex.findLastPrice("YNDX")
        assertThat(price.ticker).isEqualTo("YNDX")
        assertThat(price.price).isGreaterThan(BigDecimal.ZERO)

        val history = securitiesClientMoex.findHistoryPrices("YNDX",
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2020, 2, 1))
        assertThat(history[0].ticker).isEqualTo("YNDX")
        assertThat(history[0].price).isGreaterThan(BigDecimal.ZERO)

        val securities = securitiesClientMoex.findByName("Яндекс")
        assertThat(securities.size).isGreaterThan(0)
    }

    @Test
    @Transactional
    @Disabled
    @Rollback(false)
    fun csvLoad() {
        csvLoader.load(ClassPathResource("/csv/test2.csv"))
    }

    @Test
    @Transactional
    @Disabled
    @Rollback(false)
    fun assetHistoryLoad() {
        assetHistoryLoader.load("YNDX", LocalDate.of(2019, 1, 1), LocalDate.of(2020, 1, 1))
    }

    @Test
    @Transactional
    @Disabled
    @Rollback(false)
    fun currencyRateLoad() {
        currencyRateLoader.load(LocalDate.of(2020, 1, 1),
                LocalDate.of(2020, 2, 1))
    }

    @Test
    @Disabled
    fun security() {
        val security = investService.getSecurity("YNDX", Period.WEEK)
        assertThat(security.history.size).isGreaterThan(0)
        assertThat(security.name).contains("Yandex")
    }

    @Test
    @Disabled
    fun currency() {
        val currencyView = investService.getCurrency(RUB, Currency.USD, Period.FIVE_YEARS)
        assertThat(currencyView.history.size).isGreaterThan(0)
    }

    private fun assertThatAsset(accountId: Int, ticker: String, predicate: (AssetView) -> Boolean): AssertContinue<AssetView> {
        val found = investService.getAccount(accountId).assets.find { it.assetTicker == ticker }!!
        assertThat(found).matches(predicate)
        return AssertContinue(found)
    }

    private fun assertThatDial(accountId: Int, ticker: String, date: LocalDate, predicate: (DialView) -> Boolean): AssertContinue<DialView> {
        val found = investService.getDials(accountId, ticker).find { it.dt == date }!!
        assertThat(found).matches(predicate)
        return AssertContinue(found)
    }

    private fun assertThatDial(dialId: Long, predicate: (DialView) -> Boolean): AssertContinue<DialView> {
        val dial = dialRepo.findByIdOrNull(dialId)!!
        val found = investService.getDials(dial.accountId, dial.ticker).find {
            it.dt == dial.date && it.type == dial.type && it.active == dial.active
        }!!
        assertThat(found).matches(predicate)
        return AssertContinue(found)
    }
}

class AssertContinue<T>(private val value: T) {

    fun and(predicate: (T) -> Boolean) {
        assertThat(predicate(value))
    }
}
