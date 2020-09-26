package com.github.iryabov.invest

import com.github.iryabov.invest.service.impl.fillChart
import com.github.iryabov.invest.service.impl.money
import com.github.iryabov.invest.service.impl.normalizeProportions
import com.github.iryabov.invest.service.impl.percent
import org.assertj.core.api.Assert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Period

class ChartUtilsTest {
    @Test
    fun testFillChartByMonth() {
        val history = listOf(
                Model(LocalDate.of(2020, 1, 1), 1),
                Model(LocalDate.of(2020, 1, 10), 2),
                Model(LocalDate.of(2020, 1, 30), 3),
                Model(LocalDate.of(2020, 2, 1), 4),
                Model(LocalDate.of(2020, 4, 10), 5),
                Model(LocalDate.of(2020, 5, 30), 6))
        val chart = fillChart(history,
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2020, 6, 1),
                Period.ofMonths(1),
                { it.date },
                { d, _ -> Model(d) })

        assertThat(chart.size).isEqualTo(5)
        assertThat(chart[0].date).isEqualTo(LocalDate.of(2020, 1, 30))
        assertThat(chart[0].quantity).isEqualTo(3)
        assertThat(chart[1].date).isEqualTo(LocalDate.of(2020, 2, 1))
        assertThat(chart[1].quantity).isEqualTo(4)
        assertThat(chart[2].date).isEqualTo(LocalDate.of(2020, 3, 1))
        assertThat(chart[2].quantity).isEqualTo(0)
        assertThat(chart[3].date).isEqualTo(LocalDate.of(2020, 4, 10))
        assertThat(chart[3].quantity).isEqualTo(5)
        assertThat(chart[4].date).isEqualTo(LocalDate.of(2020, 5, 30))
        assertThat(chart[4].quantity).isEqualTo(6)
    }

    @Test
    fun testFillChartByWeek() {
        val history = listOf(
                Model(LocalDate.of(2020, 7, 2), 1),
                Model(LocalDate.of(2020, 7, 2), 2),
                Model(LocalDate.of(2020, 7, 3), 3),
                Model(LocalDate.of(2020, 7, 7), 4),
                Model(LocalDate.of(2020, 7, 8), 5),
                Model(LocalDate.of(2020, 8, 1), 6))
        val chart = fillChart(history,
                LocalDate.of(2020, 7, 1),
                LocalDate.of(2020, 7, 8),
                Period.ofDays(1),
                { it.date },
                { d, prev -> Model(d, prev?.quantity ?: 0) },
                { a, b -> a })

        assertThat(chart.size).isEqualTo(7)
        assertThat(chart[0].date).isEqualTo(LocalDate.of(2020, 7, 1))
        assertThat(chart[0].quantity).isEqualTo(0)
        assertThat(chart[1].date).isEqualTo(LocalDate.of(2020, 7, 2))
        assertThat(chart[1].quantity).isEqualTo(1)
        assertThat(chart[2].date).isEqualTo(LocalDate.of(2020, 7, 3))
        assertThat(chart[2].quantity).isEqualTo(3)
        assertThat(chart[3].date).isEqualTo(LocalDate.of(2020, 7, 4))
        assertThat(chart[3].quantity).isEqualTo(3)
        assertThat(chart[4].date).isEqualTo(LocalDate.of(2020, 7, 5))
        assertThat(chart[4].quantity).isEqualTo(3)
        assertThat(chart[5].date).isEqualTo(LocalDate.of(2020, 7, 6))
        assertThat(chart[5].quantity).isEqualTo(3)
        assertThat(chart[6].date).isEqualTo(LocalDate.of(2020, 7, 7))
        assertThat(chart[6].quantity).isEqualTo(4)
    }

    @Test
    fun normalizeProportion() {
        val result = normalizeProportions(mapOf(
                "a" to percent(25),
                "b" to percent(25),
                "c" to percent(10),
                "d" to percent(50),
                "e" to percent(0)), "d")
        assertThat(result["d"]).isNull()
        assertThat(result["e"]).isNull()
        assertThat(result["a"]).isEqualTo(percent(21))
        assertThat(result["b"]).isEqualTo(percent(21))
        assertThat(result["c"]).isEqualTo(percent(8))
    }
}

data class Model(val date: LocalDate, val quantity: Int = 0)