package com.github.iryabov.invest.model

import com.github.iryabov.invest.relation.AssetClass
import java.math.BigDecimal

/**
 * Счет
 */
data class AccountView(
        var id: Int,
        var name: String,
        /**
         * Активы счета
         */
        var assets: List<AssetView>
) {
    /**
     * Активы в виде валюты
     */
    lateinit var currencies: List<AssetView>
    /**
     * Активы в виде ценных бумаг
     */
    lateinit var securities: List<AssetView>
    /**
     * Суммарная себестоимость активов
     */
    lateinit var totalNetValue: BigDecimal
    /**
     * Суммарные средства заведенные на счет
     */
    lateinit var totalDeposit: BigDecimal
    /**
     * Суммарные средства выведенные со счета
     */
    lateinit var totalWithdrawals: BigDecimal
    /**
     * Суммарные потраченные средства
     */
    lateinit var totalExpenses: BigDecimal
    /**
     * Суммарные вырученные средства
     */
    lateinit var totalProceeds: BigDecimal
    /**
     * Суммарная рыночная стоимость всех активов
     */
    lateinit var totalMarketValue: BigDecimal
    /**
     * Изменение стоимости по рыночному курсу
     */
    lateinit var totalValueProfit: BigDecimal
    /**
     * Процент изменения стоимости по рыночному курсу
     */
    lateinit var totalValueProfitPercent: BigDecimal
    /**
     * Фиксированная прибыль
     */
    lateinit var totalFixedProfit: BigDecimal
    /**
     * Процент фиксированной прибыли относительно депозита
     */
    lateinit var totalFixedProfitPercent: BigDecimal
    /**
     * Процент фиксированной прибыли относительно оборота
     */
    lateinit var totalFixedTurnoverProfitPercent: BigDecimal
    /**
     * Полная "бумажная" прибыль
     */
    lateinit var totalMarketProfit: BigDecimal
    /**
     * Процент полной прибыли относительно депозита
     */
    lateinit var totalMarketProfitPercent: BigDecimal
    /**
     * Процент полной прибыли относительно оборота
     */
    lateinit var totalMarketTurnoverProfitPercent: BigDecimal
}