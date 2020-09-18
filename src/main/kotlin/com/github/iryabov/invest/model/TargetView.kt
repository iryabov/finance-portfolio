package com.github.iryabov.invest.model

import com.github.iryabov.invest.relation.TargetType

data class TargetView(
        val type: TargetType,
        val ticker: String,
        var assets: List<AssetView>
): ValueView()