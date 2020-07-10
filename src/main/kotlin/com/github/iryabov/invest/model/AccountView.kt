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
     * Процент фиксированной прибыли
     */
    lateinit var totalFixedProfitPercent: BigDecimal
    /**
     * Полная "бумажная" прибыль
     */
    lateinit var totalMarketProfit: BigDecimal
    /**
     * Процент полной прибыли
     */
    lateinit var totalMarketProfitPercent: BigDecimal
}

/**
 * Актив
 */
data class AssetView(
        /**
         * Тикер актива
         */
        var assetTicker: String,
        /**
         * Наименование актива
         */
        var assetName: String? = null,
        /**
         * Класс актива
         */
        var assetClass: AssetClass? = null,
        /**
         * Цена на данный момент
         */
        var assetPriceNow: BigDecimal? = null,
        /**
         * Количество актива
         */
        var quantity: Int,
        /**
         * Себестоимость активов
         */
        var netValue: BigDecimal,
        /**
         * Затраты на приобретение
         */
        var expenses: BigDecimal = BigDecimal.ZERO,
        /**
         * Выручка с продажи или дивидендов
         */
        var proceeds: BigDecimal = BigDecimal.ZERO,
        /**
         * Средства заевденные на счет
         */
        var deposit: BigDecimal = BigDecimal.ZERO,
        /**
         * Средства выведенные со счета
         */
        var withdrawals: BigDecimal = BigDecimal.ZERO) {
    /**
     * Рыночная стоимость по текущему курсу
     */
    lateinit var marketValue: BigDecimal
    /**
     * Процент прибыли курсовой (рыночная стоимость относительно себестоимости)
     */
    lateinit var valueProfit: BigDecimal
    /**
     * Процент прибыли фиксированной (себестоимость + доходы относительно расходов)
     */
    lateinit var fixedProfit: BigDecimal
    /**
     * Процент полной прибыли (рыночная стоимость + доходы относительно расходов)
     */
    lateinit var marketProfit: BigDecimal
    /**
     * Процент доли по себестоимости относительно других активов
     */
    lateinit var netInterest: BigDecimal
    /**
     * Процент доли по рыночной стоимости относительно других активов
     */
    lateinit var marketInterest: BigDecimal
    /**
     * Процент изменения доли по рыночной стоимости относительно других активов
     */
    lateinit var profitInterest: BigDecimal
}