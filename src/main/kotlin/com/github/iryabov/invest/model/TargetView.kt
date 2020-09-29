package com.github.iryabov.invest.model

import com.github.iryabov.invest.relation.TargetType

data class TargetView(
        val type: TargetType,
        val ticker: String,
        val name: String?,
        var assets: List<AssetView>
): ValueView() {
    val quantity: Int
        get() = assets.filter { it.quantity > 0 }.count()
}