package com.github.iryabov.invest.model

import com.github.iryabov.invest.relation.AssetClass
import com.github.iryabov.invest.relation.Country
import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.relation.Sector
import com.github.iryabov.invest.service.impl.P0
import java.math.BigDecimal

data class BalancedAssetView(
        val ticker: String,
        val name: String?,
        val marketValue: BigDecimal,
        val netValue: BigDecimal,
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
        var assetCurrency: Currency? = null
) {
    /**
     * Процент доли по себестоимости относительно других активов
     */
    lateinit var netProportion: BigDecimal
    /**
     * Процент доли по рыночной стоимости относительно других активов
     */
    lateinit var marketProportion: BigDecimal
    /**
     * Процент отклонения по классам активов от целевой доли по рыночной стоимости относительно других активов
     */
    var targetClassDeviationPercent: BigDecimal = P0
    /**
     * Процент отклонения по сектору экономики от целевой доли по рыночной стоимости относительно других активов
     */
    var targetSectorDeviationPercent: BigDecimal = P0
    /**
     * Процент отклонения по странам активов от целевой доли по рыночной стоимости относительно других активов
     */
    var targetCountryDeviationPercent: BigDecimal = P0
    /**
     * Процент отклонения по валютам активов от целевой доли по рыночной стоимости относительно других активов
     */
    var targetCurrencyDeviationPercent: BigDecimal = P0
    /**
     * Суммарный процент отклонения от целевой доли по рыночной стоимости относительно других активов
     */
    var totalTargetDeviationPercent: BigDecimal = P0
}