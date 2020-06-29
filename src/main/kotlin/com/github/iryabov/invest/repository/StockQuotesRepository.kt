package com.github.iryabov.invest.repository

import com.github.iryabov.invest.relation.Currency
import java.math.BigDecimal
import java.time.LocalDate

interface StockQuotesRepository {
    fun findCurrencyByBaseAndDate(base: Currency, date: LocalDate): ExchangeRate
}

data class ExchangeRate(
        val date: LocalDate,
        val base: Currency,
        val rates: Map<Currency, BigDecimal>
) {
    fun getPairExchangePrice(purchased: Currency, sale: Currency): BigDecimal {
        return when (base) {
            purchased -> {
                rates.getOrDefault(sale, BigDecimal.ZERO)
            }
            sale -> {
                BigDecimal(1.000000001) / rates.getOrDefault(purchased, BigDecimal.ZERO)
            }
            else -> {
                rates.getOrDefault(sale, BigDecimal.ZERO) / rates.getOrDefault(purchased, BigDecimal.ZERO)
            }
        }
    }
}
