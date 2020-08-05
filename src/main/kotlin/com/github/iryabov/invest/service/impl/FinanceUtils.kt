package com.github.iryabov.invest.service.impl

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

val N1 = BigDecimal(-1)
val P100 = BigDecimal(100)
val P0 = BigDecimal.ZERO
val P1 = BigDecimal.ONE

fun calcProfitPercent(a: BigDecimal, b: BigDecimal): BigDecimal {
    return if (b.compareTo(P0) != 0) a.divide(b, 2, RoundingMode.HALF_UP) * P100 - P100 else P0
}

fun calcPercent(a: BigDecimal, b: BigDecimal): BigDecimal {
    return if (b.compareTo(P0) != 0) a.divide(b, 2, RoundingMode.HALF_UP) * P100 else P0
}

inline fun <T> Iterable<T>.sumByBigDecimal(selector: (T) -> BigDecimal): BigDecimal {
    var sum: BigDecimal = BigDecimal.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

fun notZero(a: BigDecimal, b: BigDecimal): BigDecimal {
    return when {
        a.notZero() -> a
        b.notZero() -> b
        else -> a
    }
}

fun max(a: BigDecimal?, b: BigDecimal?): BigDecimal? {
    return when {
        a?.greater(b ?: P0) ?: false -> a
        b?.greater(a ?: P0) ?: false -> b
        else -> a
    }
}

fun maxAbs(a: Int, b: Int): Int {
    return when {
        abs(a) > abs(b) -> a
        abs(a) < abs(b) -> b
        else -> a
    }
}

fun BigDecimal.notZero(): Boolean {
    return !this.eq(P0)
}

fun BigDecimal.eq(other: BigDecimal, scale: Int = 0): Boolean {
    return this.setScale(scale, RoundingMode.HALF_UP).compareTo(other.setScale(scale, RoundingMode.HALF_UP)) == 0
}

fun BigDecimal.less(other: BigDecimal, scale: Int = 0): Boolean {
    return this.setScale(scale, RoundingMode.HALF_UP) < other.setScale(scale, RoundingMode.HALF_UP)
}

fun BigDecimal.greater(other: BigDecimal, scale: Int = 0): Boolean {
    return this.setScale(scale, RoundingMode.HALF_UP) > other.setScale(scale, RoundingMode.HALF_UP)
}

fun BigDecimal.round(scale: Int = 0): BigDecimal {
    return this.setScale(scale, RoundingMode.HALF_UP)
}

fun BigDecimal.invert(): BigDecimal {
    return BigDecimal(1.000000001) / this
}

fun Int.negate(): Int {
    return -1 * this
}

fun Int.invert(): Int {
    return 1 / this
}