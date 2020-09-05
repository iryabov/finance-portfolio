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
        var assets: List<AssetView>,
        /**
         * Активы в виде валюты
         */
        var currencies: List<AssetView>? = null,
        /**
         * Суммарная себестоимость активов
         */
        var securities: List<AssetView>? = null
): ValueView() {

}