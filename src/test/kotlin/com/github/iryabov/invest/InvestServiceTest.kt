package com.github.iryabov.invest

import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.model.AccountForm
import com.github.iryabov.invest.model.DialForm
import com.github.iryabov.invest.relation.DialType
import com.github.iryabov.invest.repository.AccountRepository
import com.github.iryabov.invest.repository.DialRepository
import com.github.iryabov.invest.repository.RateRepository
import com.github.iryabov.invest.repository.StockQuotesRepository
import com.github.iryabov.invest.service.InvestService
import com.github.iryabov.invest.service.impl.DialsCsvReader
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
        val csvReader: DialsCsvReader,
        @Autowired
        val stockQuotesRepo: StockQuotesRepository,
        @Autowired
        val rateRepo: RateRepository
) {

    @BeforeEach
    internal fun setUp() {
        accountRepo.deleteAll()
//        rateRepo.deleteAll()
    }

    @Test
    @Transactional
    @Disabled
    fun test() {
        //account creating
        val accountId = investService.createAccount(AccountForm("MyBrocker", "12345"))
        assertThat(accountRepo.findById(accountId)).isPresent

        //dial creating
        val dialId = investService.addDial(accountId, DialForm(type = DialType.PURCHASE,
                ticker = "TEST",
                amount = BigDecimal(100),
                currency = Currency.RUB,
                quantity = 10))
        assertThat(dialRepo.findById(dialId)).matches { d ->
            d.get().ticker == "TEST" && d.get().accountId == accountId && d.get().active
        }

        //dial deactivating
        investService.deactivateDial(accountId, dialId)
        assertThat(dialRepo.findById(dialId)).matches { d -> !d.get().active }

        //dial activating
        investService.deactivateDial(accountId, dialId)
        assertThat(dialRepo.findById(dialId)).matches { d -> d.get().active }

        //dial deleting
        investService.deleteDial(accountId, dialId)
        assertThat(dialRepo.findById(dialId).isEmpty).isTrue()

        //account deleting
        investService.deleteAccount(accountId)
        assertThat(accountRepo.findById(accountId).isEmpty).isTrue()
    }

    @Test()
    @Transactional
//    @Disabled
    fun getAccountView() {
        csvReader.read(ClassPathResource("/csv/test.csv"))
        val bank = investService.getAccount(accountRepo.findByName("Bank")!!.id!!)
        assertThat(bank.assets.size).isEqualTo(4)
        assertThat(bank.totalDeposit.eq(BigDecimal("101000"))).isTrue()
        assertThat(bank.totalWithdrawals.eq(BigDecimal("82132"))).isTrue()
        assertThat(bank.totalNetValue.eq(BigDecimal("29777"))).isTrue()
        assertThat(bank.totalFixedProfit.eq(BigDecimal("3.2"), 1)).isTrue()

        val broker = investService.getAccount(accountRepo.findByName("Broker")!!.id!!)
        assertThat(broker.assets.size).isEqualTo(5)
        assertThat(broker.totalDeposit.eq(BigDecimal("82132"))).isTrue()
        assertThat(broker.totalWithdrawals.eq(BigDecimal("1000"))).isTrue()
        assertThat(broker.totalNetValue.eq(BigDecimal("89009"))).isTrue()
        assertThat(broker.totalFixedProfit.eq(BigDecimal("4.8"), 1)).isTrue()
    }


    @Test
    @Disabled
    fun exchange() {
        val exchange = stockQuotesRepo.findCurrencyByBaseAndDate(Currency.RUB, LocalDate.of(2020, Month.JANUARY, 1))
        assertThat(exchange.getPairExchangePrice(Currency.USD, Currency.RUB).setScale(3, RoundingMode.HALF_DOWN))
                .isEqualTo(BigDecimal(62.272).setScale(3, RoundingMode.HALF_DOWN))
        assertThat(exchange.getPairExchangePrice(Currency.RUB, Currency.USD).setScale(3, RoundingMode.HALF_DOWN))
                .isEqualTo(BigDecimal(0.016).setScale(3, RoundingMode.HALF_DOWN))
        assertThat(exchange.getPairExchangePrice(Currency.EUR, Currency.USD).setScale(3, RoundingMode.HALF_DOWN))
                .isEqualTo(BigDecimal(1.123).setScale(3, RoundingMode.HALF_DOWN))
    }

    @Test
    @Transactional
    @Disabled
    @Rollback(false)
    fun read() {
        csvReader.read(ClassPathResource("/csv/test.csv"))
    }
}