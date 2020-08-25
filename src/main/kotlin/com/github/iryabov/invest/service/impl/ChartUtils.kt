package com.github.iryabov.invest.service.impl

import java.math.BigDecimal
import java.time.LocalDate
import java.util.stream.Collectors

/**
 * The list is evenly filled with dates by period
 */
fun <T> fillChart(history: List<T>, fromDate: LocalDate, tillDate: LocalDate,
                  step: java.time.Period,
                  extractor: (T) -> LocalDate,
                  constructor: (LocalDate, T?) -> T,
                  aggregator: (T, T) -> T = { a, b -> b }): List<T> {
    val result = ArrayList<T>()
    val intervals = split(fromDate, tillDate, step)
    val historyCopy = ArrayList(history)
    for (pair in intervals) {
        val list = historyCopy.filter { pair.contains(extractor(it)) }
        if (list.isNotEmpty()) {
            val t = list.reduce { a, b -> aggregator(a, b) }
            result.add(t)
            historyCopy.removeAll(list)
        } else {
            result.add(constructor(pair.first, if (result.isNotEmpty()) result.last() else null))
        }
    }
    return result
}

fun split(from: LocalDate, till: LocalDate,
          step: java.time.Period = java.time.Period.ofDays(1)): List<Pair<LocalDate, LocalDate>> {
    val result = ArrayList<Pair<LocalDate, LocalDate>>()
    val datesUntil = from.datesUntil(till, step).collect(Collectors.toList())
    var prev: LocalDate = from
    for (curr in datesUntil.subList(1, datesUntil.lastIndex + 1)) {
        result.add(Pair(prev, curr))
        prev = curr
    }
    result.add(Pair(prev, till))
    return result
}

fun Pair<LocalDate, LocalDate>.contains(date: LocalDate): Boolean {
    return date >= this.first && date < this.second
}