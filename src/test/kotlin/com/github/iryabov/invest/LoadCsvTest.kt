package com.github.iryabov.invest

import com.github.iryabov.invest.etl.DealsCsvLoader
import com.github.iryabov.invest.repository.AccountRepository
import com.github.iryabov.invest.repository.DealRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.FileSystemResource
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.jdbc.Sql
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = ["spring.datasource.url=jdbc:postgresql://localhost:5432/invest"])
@Sql("/schema.sql")
class LoadCsvTest(
        @Autowired
        val dealRepo: DealRepository,
        @Autowired
        val accountRepo: AccountRepository,
        @Autowired
        val csvLoader: DealsCsvLoader
) {
    @BeforeEach
    internal fun setUp() {
        val alfabank = accountRepo.findByName("Альфа Банк")
        if (alfabank != null)
            accountRepo.delete(alfabank)
        val tinkoff = accountRepo.findByName("Тинькофф")
        if (tinkoff != null)
            accountRepo.delete(tinkoff)
//        val vtbiis = accountRepo.findByName("ВТБ ИИС")
//        if (vtbiis != null)
//            accountRepo.delete(vtbiis!!)
//        val vtbbroker = accountRepo.findByName("ВТБ Брокер")
//        if (vtbbroker != null)
//            accountRepo.delete(vtbbroker!!)
        val vtb = accountRepo.findByName("ВТБ")
        if (vtb != null)
            accountRepo.delete(vtb)
    }

    @Test
    @Transactional
    //@Disabled
    @Rollback(false)
    fun csvLoad() {
        csvLoader.load(FileSystemResource("excel/test5.csv"))
    }

}