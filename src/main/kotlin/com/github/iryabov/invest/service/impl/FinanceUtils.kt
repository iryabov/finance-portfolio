package com.github.iryabov.invest.service.impl

import java.math.BigDecimal

val N1 = BigDecimal(-1)
val P100 = BigDecimal(100)
val P0 = BigDecimal.ZERO
val P1 = BigDecimal.ONE

fun calcIncomePercent(a: BigDecimal, b: BigDecimal): BigDecimal {
    return a / b * P100 - P100
}