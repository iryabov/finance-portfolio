package com.github.iryabov.invest.model

import java.math.BigDecimal

data class AccountView(
        val totalDeposit: BigDecimal,
        val totalWithdrawals: BigDecimal,
        val totalAssets: BigDecimal? = null,
        val totalFixedEarningPercent: BigDecimal? = null,
        val totalCourseEarningPercent: BigDecimal? = null,
        val assets: List<AccountAssetView>? = null
)

data class AccountAssetView(
        val assetTicker: String,
        val assetName: String? = null,
        val assetClass: String? = null,
        val amount: BigDecimal,
        val quantity: Int,
        val spent: BigDecimal,
        val received: BigDecimal,

        val proportionPercent: BigDecimal? = null,
        val fixedEarningPercent: BigDecimal? = null,
        val courseEarningPercent: BigDecimal? = null,
        val fixedEarning: BigDecimal? = null,
        val courseEarning: BigDecimal? = null,
        val averagePurchasePrice: BigDecimal? = null,
        val currentPrice: BigDecimal? = null,
        val changeProportionPercent: BigDecimal? = null
)