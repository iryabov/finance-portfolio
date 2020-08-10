package com.github.iryabov.invest.model

import com.github.iryabov.invest.relation.AssetClass
import com.github.iryabov.invest.service.impl.P0
import org.springframework.data.relational.core.mapping.Embedded
import java.math.BigDecimal
import java.util.*

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
        var withdrawals: BigDecimal = BigDecimal.ZERO,
        /**
         * Рыночная стоимость по текущему курсу
         */
        var marketValue: BigDecimal = P0,
        /**
         * Процент прибыли курсовой (рыночная стоимость относительно себестоимости)
         */
        var valueProfit: BigDecimal = P0,
        /**
         * Процент прибыли фиксированной относительно оборота
         */
        var fixedProfit: BigDecimal = P0,
        /**
         * Процент прибыли фиксированной и курсовой относительно оборота
         */
        var marketProfit: BigDecimal = P0,
        /**
         * Процент доли по себестоимости относительно других активов
         */
        var netInterest: BigDecimal = P0,
        /**
         * Процент доли по рыночной стоимости относительно других активов
         */
        var marketInterest: BigDecimal = P0,
        /**
         * Процент изменения доли по рыночной стоимости относительно других активов
         */
        var profitInterest: BigDecimal = P0)