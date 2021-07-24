package com.github.iryabov.invest.service.impl

import com.github.iryabov.invest.relation.Currency
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

val N1 = BigDecimal(-1)
val P100 = BigDecimal(100)
val P0: BigDecimal = BigDecimal.ZERO
val P1: BigDecimal = BigDecimal.ONE
const val OTHER = "OTHER"

fun percent(value: Int) = BigDecimal(value)
fun percent(value: Double) = BigDecimal(value)
fun money(value: Int) = BigDecimal(value)
fun money(value: Double) = BigDecimal(value)
fun date(value: String): LocalDate = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)

fun calcProfitPercent(a: BigDecimal, b: BigDecimal): BigDecimal {
    return if (b.compareTo(P0) != 0) a.divide(b, 2, RoundingMode.HALF_UP) * P100 - P100 else P0
}

fun calcPercent(a: BigDecimal, b: BigDecimal): BigDecimal {
    return if (b.compareTo(P0) != 0) a.divide(b, 2, RoundingMode.HALF_UP) * P100 else P0
}

fun calcValue(total: BigDecimal, percent: BigDecimal): BigDecimal {
    return total.multiply(percent).divide(P100, 2, RoundingMode.HALF_UP)
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

fun maxStrong(a: Int, b: Int): Int {
    return kotlin.math.max(a, b)
}

fun max(a: BigDecimal?, b: BigDecimal?): BigDecimal? {
    return when {
        a?.greater(b ?: P0) ?: false -> a
        b?.greater(a ?: P0) ?: false -> b
        else -> a
    }
}

fun maxStrong(a: BigDecimal, b: BigDecimal): BigDecimal {
    return when {
        a.greater(b) -> a
        b.greater(a) -> b
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

fun avg(a: Int, b: Int): Int {
    return (a + b) / 2
}

fun avg(a: BigDecimal, b: BigDecimal): BigDecimal {
    return (a + b) / BigDecimal(2)
}

fun BigDecimal?.notZero(): Boolean {
    return if (this != null) !this.eq(P0, 4) else false
}

fun BigDecimal?.isZero(): Boolean {
    return !notZero()
}

fun BigDecimal.eq(other: BigDecimal, scale: Int = 0): Boolean {
    return this.setScale(scale, RoundingMode.HALF_UP).compareTo(other.setScale(scale, RoundingMode.HALF_UP)) == 0
}

fun BigDecimal.notEq(other: BigDecimal, scale: Int = 0): Boolean {
    return !eq(other, scale)
}

fun BigDecimal.eqOr(first: BigDecimal, second: BigDecimal, scale: Int = 0): Boolean {
    val self = this.setScale(scale, RoundingMode.HALF_UP)
    return self.compareTo(first.setScale(scale, RoundingMode.HALF_UP)) == 0 ||
            self.compareTo(second.setScale(scale, RoundingMode.HALF_UP)) == 0
}

fun BigDecimal.eq(other: Int, scale: Int = 0): Boolean {
    return this.setScale(scale, RoundingMode.HALF_UP).compareTo(BigDecimal(other).setScale(scale, RoundingMode.HALF_UP)) == 0
}

fun BigDecimal.notEq(other: Int, scale: Int = 0): Boolean {
    return !eq(other, scale)
}

fun BigDecimal.less(other: BigDecimal, scale: Int = 0): Boolean {
    return this.setScale(scale, RoundingMode.HALF_UP) < other.setScale(scale, RoundingMode.HALF_UP)
}

fun BigDecimal.lessOrEq(other: BigDecimal, scale: Int = 0): Boolean {
    return this.setScale(scale, RoundingMode.HALF_UP) <= other.setScale(scale, RoundingMode.HALF_UP)
}

fun BigDecimal.greater(other: BigDecimal, scale: Int = 0): Boolean {
    return this.setScale(scale, RoundingMode.HALF_UP) > other.setScale(scale, RoundingMode.HALF_UP)
}

fun BigDecimal.round(scale: Int = 0): BigDecimal {
    return this.setScale(scale, RoundingMode.HALF_UP)
}

fun BigDecimal.invert(): BigDecimal {
    return if (this.notZero()) BigDecimal(1.000000001) / this else P0
}

fun Int.negate(): Int {
    return -1 * this
}

fun Int.invert(): Int {
    return 1 / this
}

fun String.isCurrency(): Boolean {
    return Currency.values().any { c -> c.name == this }
}

fun currencyOf(ticker: String): Currency? {
    return Currency.values().find { c -> c.name == ticker }
}

fun LocalDate.betweenNow(): Long {
    val days = ChronoUnit.DAYS.between(this, LocalDate.now())
    return if (days < 365) 365 else days
}