package com.github.iryabov.invest.service.impl

import java.math.BigDecimal
import java.math.RoundingMode

val N1 = BigDecimal(-1)
val P100 = BigDecimal(100)
val P0 = BigDecimal.ZERO
val P1 = BigDecimal.ONE

fun calcProfit(a: BigDecimal, b: BigDecimal): BigDecimal {
    return if (b.compareTo(P0) != 0) a / b * P100 - P100 else P0
}

fun calcPercent(a: BigDecimal, b: BigDecimal): BigDecimal {
    return if (b.compareTo(P0) != 0) a / b * P100 else P0
}

inline fun <T> Iterable<T>.sumByBigDecimal(selector: (T) -> BigDecimal): BigDecimal {
    var sum: BigDecimal = BigDecimal.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

fun BigDecimal.eq(other: BigDecimal, scale: Int = 0): Boolean {
    return this.setScale(scale, RoundingMode.HALF_UP).compareTo(other.setScale(scale, RoundingMode.HALF_UP)) == 0
}

fun BigDecimal.less(other: BigDecimal, scale: Int = 0): Boolean {
    return this.setScale(scale, RoundingMode.HALF_UP) < other.setScale(scale, RoundingMode.HALF_UP)
}

fun BigDecimal.more(other: BigDecimal, scale: Int = 0): Boolean {
    return this.setScale(scale, RoundingMode.HALF_UP) > other.setScale(scale, RoundingMode.HALF_UP)
}