package com.github.iryabov.invest.model

import java.math.BigDecimal

data class PortfolioSummaryView(
        val deposit: BigDecimal,
        val depositChange: BigDecimal,
        val withdrawals: BigDecimal,
        val withdrawalsChange: BigDecimal,
        val netValue: BigDecimal,
        val netValueChange: BigDecimal,
        val marketValue: BigDecimal,
        val marketValueChange: BigDecimal,
        val dividends: BigDecimal,
        val dividendsChange: BigDecimal,
        val coupons: BigDecimal,
        val couponsChange: BigDecimal,
        val percents: BigDecimal,
        val percentsChange: BigDecimal,
        val trading: BigDecimal,
        val tradingChange: BigDecimal,
        val marketProfit: BigDecimal,
        val marketProfitChange: BigDecimal,
        val grossProfit: BigDecimal,
        val grossProfitChange: BigDecimal,
        val netProfit: BigDecimal,
        val netProfitChange: BigDecimal,
        val tax: BigDecimal,
        val taxChange: BigDecimal,
        val fee: BigDecimal,
        val feeChange: BigDecimal
) {
    val totalProfit: BigDecimal = grossProfit + marketProfit
    val totalProfitChange: BigDecimal = grossProfitChange + marketProfitChange
}