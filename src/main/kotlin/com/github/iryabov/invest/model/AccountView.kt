package com.github.iryabov.invest.model

import java.math.BigDecimal

data class AccountView(
        val assets: List<AssetView>
) {
    lateinit var totalAmount: BigDecimal
    lateinit var totalDeposit: BigDecimal
    lateinit var totalWithdrawals: BigDecimal
    lateinit var totalSpent: BigDecimal
    lateinit var totalReceived: BigDecimal
    lateinit var totalAmountCourse: BigDecimal
    lateinit var totalProfitCourse: BigDecimal
    lateinit var totalProfitFix: BigDecimal
    lateinit var totalProfit: BigDecimal
}

data class AssetView(
        val assetTicker: String,
        val assetName: String? = null,
        val assetClass: String? = null,
        val assetPriceNow: BigDecimal? = null,
        val quantity: Int,
        val amount: BigDecimal,
        val spent: BigDecimal = BigDecimal.ZERO,
        val received: BigDecimal = BigDecimal.ZERO,
        val deposit: BigDecimal = BigDecimal.ZERO,
        val withdrawals: BigDecimal = BigDecimal.ZERO) {

    lateinit var amountCourse: BigDecimal
    lateinit var profitCourse: BigDecimal
    lateinit var profitFix: BigDecimal
    lateinit var profit: BigDecimal
    lateinit var proportion: BigDecimal
    lateinit var proportionCourse: BigDecimal
    lateinit var proportionProfit: BigDecimal
}