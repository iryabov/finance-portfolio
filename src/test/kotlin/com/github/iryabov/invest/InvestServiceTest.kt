package com.github.iryabov.invest

import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.model.AccountForm
import com.github.iryabov.invest.model.DialForm
import com.github.iryabov.invest.relation.DialType
import com.github.iryabov.invest.repository.AccountRepository
import com.github.iryabov.invest.repository.DialRepository
import com.github.iryabov.invest.service.InvestService
import com.github.iryabov.invest.service.impl.DialsCsvReader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.jdbc.Sql
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

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
        val csvReader: DialsCsvReader
) {

    @BeforeEach
    internal fun setUp() {
        accountRepo.deleteAll()
    }

    @Test
    @Transactional
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

    @Test
    @Transactional
    fun getAccountView() {
        csvReader.read(ClassPathResource("/csv/test.csv"))
        val bank = investService.getAccount(accountRepo.findByName("Bank")!!.id!!)
        assertThat(bank.totalDeposit).isEqualTo(BigDecimal("101000"))
        assertThat(bank.totalWithdrawals).isEqualTo(BigDecimal("50000"))
        assertThat(bank.assets?.size).isEqualTo(2)

        val broker = investService.getAccount(accountRepo.findByName("Broker")!!.id!!)
        assertThat(broker.totalDeposit).isEqualTo(BigDecimal("50000"))
        assertThat(broker.totalWithdrawals).isEqualTo(BigDecimal("1000"))
    }
}