package com.github.iryabov.invest.model

import java.math.BigDecimal

data class BalancedView(
        val totalMarketValue: BigDecimal,
        val balance: BigDecimal,
        val deviation: BigDecimal,
        var assets: List<BalancedAssetView>
)