package com.github.iryabov.invest.relation

import java.time.LocalDate

enum class Period(val from: () -> LocalDate, val step: java.time.Period, val interval: String) {
    WEEK({ LocalDate.now().minusWeeks(1) }, java.time.Period.ofDays(1), "1 day"),
    MONTH({ LocalDate.now().minusMonths(1) }, java.time.Period.ofDays(1), "2 days"),
    HALF_YEAR({ LocalDate.now().minusMonths(6) }, java.time.Period.ofWeeks(1), "4 days"),
    YEAR({ LocalDate.now().minusYears(1) }, java.time.Period.ofWeeks(2), "6 days"),
    FIVE_YEARS({ LocalDate.now().minusYears(5) }, java.time.Period.ofMonths(1), "8 days")
}