package com.github.iryabov.invest.model

import com.github.iryabov.invest.service.impl.P0
import java.math.BigDecimal
import java.time.LocalDate

data class AssetHistoryView(
        val date: LocalDate,
        var price: BigDecimal = P0,
        var securityPrice: BigDecimal? = null,
        var quantity: Int = 0
) {
    var totalNetValue: BigDecimal = P0
    var totalMarketValue: BigDecimal = P0

    val purchase: Int
        get() = if (quantity > 0) quantity else 0

    val sale: Int
        get() = if (quantity < 0) quantity else 0

    val purchasePrice: BigDecimal?
        get() = if (quantity > 0) price else null

    val salePrice: BigDecimal?
        get() = if (quantity < 0) price else null
}