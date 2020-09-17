package com.github.iryabov.invest.model

import java.math.BigDecimal

abstract class ValueView {
    /**
     * Суммарные потраченные средства
     */
    lateinit var totalExpenses: BigDecimal
    /**
     * Суммарные вырученные средства
     */
    lateinit var totalProceeds: BigDecimal
    /**
     * Суммарные средства заведенные на счет
     */
    lateinit var totalDeposit: BigDecimal
    /**
     * Суммарные средства выведенные со счета
     */
    lateinit var totalWithdrawals: BigDecimal
    /**
     * Суммарная себестоимость активов
     */
    lateinit var totalNetValue: BigDecimal
    /**
     * Суммарная рыночная стоимость всех активов
     */
    lateinit var totalMarketValue: BigDecimal
    /**
     * Фиксированная прибыль
     */
    lateinit var totalFixedProfit: BigDecimal
    /**
     * Полная "бумажная" прибыль
     */
    lateinit var totalMarketProfit: BigDecimal
    /**
     * Процент фиксированной прибыли относительно депозита
     */
    lateinit var totalDepositFixedProfitPercent: BigDecimal
    /**
     * Процент полной прибыли относительно депозита
     */
    lateinit var totalDepositMarketProfitPercent: BigDecimal

    /**
     * Изменение стоимости по рыночному курсу
     */
    lateinit var totalValueProfit: BigDecimal
    /**
     * Процент изменения стоимости по рыночному курсу
     */
    lateinit var totalDepositValueProfitPercent: BigDecimal
    /**
     * Сумма целей в процентах
     */
    lateinit var totalTargetProportion: BigDecimal
    /**
     * Сумма рыночной стоимости в процентах
     */
    lateinit var totalMarketProportion: BigDecimal
    /**
     * Сумма себестоимости в процентах
     */
    lateinit var totalNetProportion: BigDecimal
}