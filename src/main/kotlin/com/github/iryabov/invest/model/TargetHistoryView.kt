package com.github.iryabov.invest.model

import com.github.iryabov.invest.service.impl.P0
import java.math.BigDecimal
import java.time.LocalDate

data class TargetHistoryView(
        val date: LocalDate,
        var netValue: BigDecimal = P0,
        var marketValue: BigDecimal = P0,
        var dividendValue: BigDecimal = P0,
        var assets: List<CumulativeAssetHistoryView>
)

data class CumulativeAssetHistoryView(
        val date: LocalDate,
        val ticker: String,
        val quantity: Int = 0,
        val netValue: BigDecimal = P0,
        val profit: BigDecimal = P0
)

data class TargetSecurityHistoryView(
        val date: LocalDate,
        val securities: List<SecuritiesHistoryView>
)