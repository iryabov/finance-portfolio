package com.github.iryabov.invest.model

import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.relation.DealType
import com.github.iryabov.invest.service.impl.P0
import com.github.iryabov.invest.service.impl.betweenNow
import com.github.iryabov.invest.service.impl.notZero
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant.now
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

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

    val dividendProfitPerYear: BigDecimal?
        get() = if (volume.notZero()) (dividendProfit * BigDecimal(100))
                .divide(volume!!, 2, RoundingMode.HALF_UP)
                .divide(BigDecimal(dt.betweenNow()), 2, RoundingMode.HALF_UP) * BigDecimal(365)
        else null
}