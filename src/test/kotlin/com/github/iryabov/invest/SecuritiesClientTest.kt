package com.github.iryabov.invest

import com.github.iryabov.invest.client.impl.SecuritiesClientMoex
import com.github.iryabov.invest.client.impl.SecuritiesClientUnibit
import com.github.iryabov.invest.service.impl.money
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = ["spring.datasource.url=jdbc:postgresql://localhost:5432/invest_test"])
class SecuritiesClientTest(
        @Autowired
        @Qualifier("securitiesClientMoex")
        val securitiesClientMoex: SecuritiesClientMoex,
        @Autowired
        @Qualifier("securitiesClientUnibit")
        val securitiesClientUnibit: SecuritiesClientUnibit
) {

    @Test
    fun findHistory() {
        val yandex = securitiesClientMoex.findHistoryPrices("YNDX",
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2020, 2, 1))
        Assertions.assertThat(yandex[0].ticker).isEqualTo("YNDX")
        Assertions.assertThat(yandex[0].settlementPrice).isEqualTo(money(2685))

        val fxus = securitiesClientMoex.findHistoryPrices("FXUS",
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2020, 2, 1))
        Assertions.assertThat(fxus[0].ticker).isEqualTo("FXUS")
        Assertions.assertThat(fxus[0].settlementPrice).isEqualTo(money(3575))

        val ofz = securitiesClientMoex.findHistoryPrices("ОФЗ 26220",
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2020, 2, 1))
        Assertions.assertThat(ofz[0].ticker).isEqualTo("SU26220RMFS2")
        Assertions.assertThat(ofz[0].settlementPrice).isGreaterThan(money(100))

        val vtbb = securitiesClientMoex.findHistoryPrices("ВТБ Б-1-85",
                LocalDate.of(2020, 7, 1),
                LocalDate.of(2020, 8, 1))
        Assertions.assertThat(vtbb[0].ticker).isEqualTo("RU000A101U46")
        Assertions.assertThat(vtbb[0].settlementPrice).isGreaterThan(money(100))

        val apple = securitiesClientUnibit.findHistoryPrices("AAPL",
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2020, 2, 1))
        Assertions.assertThat(apple[0].ticker).isEqualTo("AAPL")
        Assertions.assertThat(apple[0].settlementPrice).isEqualTo(money(300.35))
    }

    @Test
    fun findPrice() {
        val yandex = securitiesClientMoex.findLastPrice("YNDX")
        Assertions.assertThat(yandex.ticker).isEqualTo("YNDX")
        Assertions.assertThat(yandex.settlementPrice).isGreaterThan(BigDecimal.ZERO)

        val fxus = securitiesClientMoex.findLastPrice("FXUS")
        Assertions.assertThat(fxus.ticker).isEqualTo("FXUS")
        Assertions.assertThat(fxus.settlementPrice).isGreaterThan(BigDecimal.ZERO)

        val ofz = securitiesClientMoex.findLastPrice("ОФЗ 26220")
        Assertions.assertThat(ofz.ticker).isEqualTo("SU26220RMFS2")
        Assertions.assertThat(ofz.settlementPrice).isGreaterThan(BigDecimal.ZERO)

        val apple = securitiesClientUnibit.findLastPrice("AAPL")
        Assertions.assertThat(apple.ticker).isEqualTo("AAPL")
        Assertions.assertThat(apple.settlementPrice).isEqualTo(money(497.48))
    }

    @Test
    fun findSecurities() {
        val yandex = securitiesClientMoex.findByName("Яндекс")
        Assertions.assertThat(yandex.size).isGreaterThan(0)

        val fxus = securitiesClientMoex.findByName("FXUS")
        Assertions.assertThat(fxus.size).isGreaterThan(0)

        val ofz = securitiesClientMoex.findByName("ОФЗ 26220")
        Assertions.assertThat(ofz.size).isGreaterThan(0)
    }
}