package com.github.iryabov.invest.relation

import java.time.LocalDate

enum class Period(val from: () -> LocalDate) {
    WEEK({ LocalDate.now().minusWeeks(1) }),
    MONTH({ LocalDate.now().minusMonths(1) }),
    HALF_YEAR({ LocalDate.now().minusMonths(6) }),
    YEAR(({ LocalDate.now().minusYears(1) })),
    FIVE_YEARS({ LocalDate.now().minusYears(5) })
}