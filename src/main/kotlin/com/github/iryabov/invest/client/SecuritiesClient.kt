package com.github.iryabov.invest.client

import com.github.iryabov.invest.relation.Currency
import java.math.BigDecimal
import java.time.LocalDate

interface SecuritiesClient {
    fun findHistoryPrices(name: String, from: LocalDate, till: LocalDate = LocalDate.now()): List<Security>

    fun findLastPrice(name: String): Security

    fun findByName(name: String): List<Security>
}

data class Security(
        val date: LocalDate = LocalDate.now(),
        val ticker: String,
        val shortName: String = "",
        val fullName: String = "",
        var price: BigDecimal = BigDecimal.ZERO,
        val settlementCurrency: Currency = Currency.RUB,
        val faceCurrency: Currency = Currency.RUB
)