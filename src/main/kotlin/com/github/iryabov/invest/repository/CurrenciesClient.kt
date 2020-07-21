package com.github.iryabov.invest.repository

import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.service.impl.invert
import java.math.BigDecimal
import java.time.LocalDate

interface CurrenciesClient {
    fun findCurrencyByDate(date: LocalDate): ExchangeRate
}

data class ExchangeRate(
        val date: LocalDate,
        /**
         * Base currency
         */
        val base: Currency,
        /**
         * Prices for purchase base currency
         */
        val rates: Map<Currency, BigDecimal>
) {
    fun getPairExchangePrice(purchased: Currency, sale: Currency): BigDecimal {
        return when (base) {
            purchased -> {
                rates.getOrDefault(sale, BigDecimal.ZERO)
            }
            sale -> {
                rates.getOrDefault(purchased, BigDecimal.ZERO).invert()
            }
            else -> {
                rates.getOrDefault(sale, BigDecimal.ZERO) / rates.getOrDefault(purchased, BigDecimal.ZERO)
            }
        }
    }
}
