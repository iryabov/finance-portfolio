package com.github.iryabov.invest.model

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