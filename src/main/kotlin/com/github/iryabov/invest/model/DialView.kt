package com.github.iryabov.invest.model

import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.relation.DialType
import java.math.BigDecimal
import java.time.LocalDate

data class DialView(
        var id: Long,
        var active: Boolean,
        var dt: LocalDate,
        var assetTicker: String,
        var assetName: String?,
        var type: DialType,
        var quantity: Int?,
        var currency: Currency?,
        var volume: BigDecimal?,
        var price: BigDecimal?,
        var soldQuantity: Int?)