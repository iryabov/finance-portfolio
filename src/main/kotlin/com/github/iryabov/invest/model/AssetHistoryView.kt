package com.github.iryabov.invest.model

import java.math.BigDecimal
import java.time.LocalDate

data class AssetHistoryView(
        val date: LocalDate,
        val price: BigDecimal,
        val quantity: Int,
        var purchase: Int = 0,
        var sale: Int = 0
)