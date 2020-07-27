package com.github.iryabov.invest.relation

import java.time.LocalDate

enum class Period(val from: () -> LocalDate, val step: java.time.Period) {
    WEEK({ LocalDate.now().minusWeeks(1) }, java.time.Period.ofDays(1)),
    MONTH({ LocalDate.now().minusMonths(1) }, java.time.Period.ofDays(1)),
    HALF_YEAR({ LocalDate.now().minusMonths(6) }, java.time.Period.ofWeeks(1)),
    YEAR({ LocalDate.now().minusYears(1) }, java.time.Period.ofMonths(1)),
    FIVE_YEARS({ LocalDate.now().minusYears(5) }, java.time.Period.ofMonths(1))
}