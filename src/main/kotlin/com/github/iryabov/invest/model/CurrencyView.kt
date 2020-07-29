package com.github.iryabov.invest.model

import com.github.iryabov.invest.relation.Currency
import java.math.BigDecimal
import java.time.LocalDate

data class CurrencyView(
        val pair1: Currency,
        val pair2: Currency,
        var history: List<CurrencyHistoryView>
)

data class CurrencyHistoryView(
        val date: LocalDate,
        val price: BigDecimal
)