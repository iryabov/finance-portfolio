package com.github.iryabov.invest.model

import java.math.BigDecimal
import java.time.LocalDate

data class TargetBenchmarkView(
        val date: LocalDate,
        val portfolioProfit: BigDecimal,
        val benchmarkProfit: BigDecimal
)