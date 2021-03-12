package com.github.iryabov.invest.model

import com.github.iryabov.invest.service.impl.P0
import com.github.iryabov.invest.service.impl.calcProfitPercent
import java.math.BigDecimal
import java.time.LocalDate

data class TargetHistoryView(
        val date: LocalDate,
        var balance: BigDecimal = P0,
        var netValue: BigDecimal = P0,
        var marketValue: BigDecimal = P0,
        var profitValue: BigDecimal = P0,
        var quantity: Int = 0,
        var dividends: BigDecimal = P0,
        var coupons: BigDecimal = P0,
        var percents: BigDecimal = P0,
        var trading: BigDecimal = P0,
        var taxes: BigDecimal = P0,
        var deposit: BigDecimal = P0,
        var withdrawals: BigDecimal = P0
) {
    val marketProfitPercent: BigDecimal
        get(): BigDecimal = calcProfitPercent(marketValue, balance)
    val fixedProfitPercent: BigDecimal
        get(): BigDecimal = calcProfitPercent(netValue, balance)
}