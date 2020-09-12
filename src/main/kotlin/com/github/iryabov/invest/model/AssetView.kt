package com.github.iryabov.invest.model

import com.github.iryabov.invest.relation.AssetClass
import com.github.iryabov.invest.relation.Country
import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.relation.Sector
import com.github.iryabov.invest.service.impl.P0
import org.springframework.data.relational.core.mapping.Embedded
import java.math.BigDecimal
import java.util.*

/**
 * Актив
 */
data class AssetView(
        /**
         * Учитывать ли актив в расчетах
         */
        var active: Boolean = true,
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
         * Сектор актива
         */
        var assetSector: Sector? = null,
        /**
         * Страна актива
         */
        var assetCountry: Country? = null,
        /**
         * Валюта актива
         */
        var assetCurrency: Currency? = null,
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
         * Процент целевой доли по рыночной стоимости относительно других активов
         */
        val targetProportion: BigDecimal = P0,
        /**
         * Цена для фиксации прибыли
         */
        var takeProfit: BigDecimal? = null,
        /**
         * Цена для фиксации убытка
         */
        var stopLoss: BigDecimal? = null,
        /**
         * Заметка
         */
        var note: String? = null
) {
        /**
         * Процент прибыли курсовой (рыночная стоимость относительно себестоимости)
         */
        lateinit var valueProfitPercent: BigDecimal
        /**
         * Процент прибыли фиксированной относительно оборота
         */
        lateinit var fixedProfitPercent: BigDecimal
        /**
         * Процент прибыли фиксированной и курсовой относительно оборота
         */
        lateinit var marketProfitPercent: BigDecimal
        /**
         * Процент доли по себестоимости относительно других активов
         */
        lateinit var netProportion: BigDecimal
        /**
         * Процент доли по рыночной стоимости относительно других активов
         */
        lateinit var marketProportion: BigDecimal
        /**
         * Процент изменения доли по рыночной стоимости относительно других активов
         */
        lateinit var marketProfitProportion: BigDecimal
        /**
         * Отклонение от целевой доли по рыночной стоимости относительно других активов
         */
        var targetDeviation: BigDecimal? = null
        /**
         * Процент отклонения от целевой доли по рыночной стоимости относительно других активов
         */
        var targetDeviationPercent: BigDecimal? = null
}