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
import com.github.iryabov.invest.relation.Period
import com.github.iryabov.invest.service.impl.eq
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.jdbc.Sql
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.Month

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

        //dial creating
        val purchase = investService.addDial(accountId, DialForm(type = DialType.PURCHASE,
                ticker = "TEST",
                volume = BigDecimal(100),
                currency = Currency.RUB,
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

        val sale = investService.addDial(accountId, DialForm(type = DialType.SALE,
                ticker = "TEST",
                volume = BigDecimal(25),
                currency = Currency.RUB,
                quantity = 2))
        assertThat(dialRepo.findById(sale).get()).matches { d ->
            d.ticker == "TEST" && d.accountId == accountId && d.active
                    && d.quantity == -2 && d.volume == BigDecimal(25)
        }

        account = investService.getAccount(accountId)
        assertThat(account).matches {
            it.totalNetValue.eq(BigDecimal(105))
            it.assets.find { a -> a.assetTicker == "TEST" }!!.netValue.eq(BigDecimal(80))
        }

        //dial deactivating
        investService.deactivateDial(accountId, sale)
        assertThat(dialRepo.findById(sale).get()).matches { d -> !d.active }

        account = investService.getAccount(accountId)
        assertThat(account).matches {
            it.totalNetValue.eq(BigDecimal(80))
            it.assets.find { a -> a.assetTicker == "TEST" }!!.netValue.eq(BigDecimal(80))
        }

        //dial activating
        investService.deactivateDial(accountId, sale)
        assertThat(dialRepo.findById(sale).get()).matches { d -> d.active }

        account = investService.getAccount(accountId)
        assertThat(account).matches {
            it.totalNetValue.eq(BigDecimal(105))
            it.assets.find { a -> a.assetTicker == "TEST" }!!.netValue.eq(BigDecimal(80))
        }

        //dial deleting
        investService.deleteDial(accountId, purchase)
        assertThat(dialRepo.findById(purchase).isEmpty).isTrue()

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
        assertThat(exchange.getPairExchangePrice(Currency.USD, Currency.RUB).setScale(3, RoundingMode.HALF_DOWN))
                .isEqualTo(BigDecimal(62.272).setScale(3, RoundingMode.HALF_DOWN))
        assertThat(exchange.getPairExchangePrice(Currency.RUB, Currency.USD).setScale(3, RoundingMode.HALF_DOWN))
                .isEqualTo(BigDecimal(0.016).setScale(3, RoundingMode.HALF_DOWN))
        assertThat(exchange.getPairExchangePrice(Currency.EUR, Currency.USD).setScale(3, RoundingMode.HALF_DOWN))
                .isEqualTo(BigDecimal(1.123).setScale(3, RoundingMode.HALF_DOWN))
    }

    @Test
    @Disabled
    fun exchangeCBRF() {
        val exchange = currenciesClientCBRF.findCurrencyByDate(LocalDate.of(2020, Month.JANUARY, 1))
        assertThat(exchange.getPairExchangePrice(Currency.USD, Currency.RUB).setScale(3, RoundingMode.HALF_DOWN))
                .isEqualTo(BigDecimal(61.906).setScale(3, RoundingMode.HALF_DOWN))
        assertThat(exchange.getPairExchangePrice(Currency.RUB, Currency.USD).setScale(3, RoundingMode.HALF_DOWN))
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
        csvLoader.load(ClassPathResource("/csv/test.csv"))
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
//    @Disabled
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
        val currencyView = investService.getCurrency(Currency.RUB, Currency.USD, Period.FIVE_YEARS)
        assertThat(currencyView.history.size).isGreaterThan(0)
    }
}