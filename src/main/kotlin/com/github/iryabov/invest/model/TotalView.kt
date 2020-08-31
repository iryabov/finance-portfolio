package com.github.iryabov.invest.model

import java.math.BigDecimal

/**
 * Итоги по всем счетам
 */
data class TotalView(
        val accounts: List<AccountView>
) {
    lateinit var totalDeposit: BigDecimal
    lateinit var totalWithdrawals: BigDecimal
    lateinit var totalNetValue: BigDecimal
    lateinit var totalMarketValue: BigDecimal
    lateinit var totalFixedProfit: BigDecimal
    lateinit var totalMarketProfit: BigDecimal
    lateinit var totalDepositFixedProfitPercent: BigDecimal
    lateinit var totalDepositMarketProfitPercent: BigDecimal
}