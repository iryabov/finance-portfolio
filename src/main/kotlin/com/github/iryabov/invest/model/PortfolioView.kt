package com.github.iryabov.invest.model

data class PortfolioView(
        var id: Int,
        var name: String
): ValueView() {
    lateinit var assets: List<AssetView>
}