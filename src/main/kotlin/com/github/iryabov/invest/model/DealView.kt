package com.github.iryabov.invest.model

import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.relation.DealType
import com.github.iryabov.invest.service.impl.P0
import com.github.iryabov.invest.service.impl.notZero
import java.math.BigDecimal
import java.time.LocalDate

data class DealView(
        var id: Long,
        var active: Boolean,
        var dt: LocalDate,
        var assetTicker: String,
        var assetName: String?,
        var type: DealType,
        var quantity: Int?,
        var currency: Currency?,
        var volume: BigDecimal?,
        var price: BigDecimal?,
        var soldQuantity: Int?,
        var soldVolume: BigDecimal?,
        var dividendQuantity: Int?,
        var settlementTicker: String?,
        var settlementQuantity: Int?) {
    var dividendProfit: BigDecimal = P0
    var profit: BigDecimal? = if (soldVolume.notZero()) (soldVolume ?: P0) + (volume ?: P0) else null
}