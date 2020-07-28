package com.github.iryabov.invest.model

import com.github.iryabov.invest.service.impl.P0
import java.math.BigDecimal
import java.time.LocalDate

data class AssetHistoryView(
        val date: LocalDate,
        var price: BigDecimal = P0,
        var purchasePrice: BigDecimal? = null,
        var salePrice: BigDecimal? = null,
        var securityPrice: BigDecimal? = null,
        var quantity: Int = 0,
        var purchase: Int = 0,
        var sale: Int = 0
)