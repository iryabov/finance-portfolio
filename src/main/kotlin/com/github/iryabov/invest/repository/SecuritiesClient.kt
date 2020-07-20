package com.github.iryabov.invest.repository

import java.math.BigDecimal
import java.time.LocalDate

interface SecuritiesClient {
    fun findHistoryPrices(ticker: String, from: LocalDate, till: LocalDate = LocalDate.now()): List<Security>

    fun findLastPrice(ticker: String): Security
}

data class Security(
        val date: LocalDate,
        val ticker: String,
        val shortName: String,
        val price: BigDecimal
)