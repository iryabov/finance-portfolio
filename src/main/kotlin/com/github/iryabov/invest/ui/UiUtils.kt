package com.github.iryabov.invest.ui

import com.github.iryabov.invest.model.AssetView
import com.github.iryabov.invest.model.ChartView
import com.github.iryabov.invest.service.impl.color
import com.github.iryabov.invest.service.impl.percentFormat
import com.github.iryabov.invest.service.impl.profitFormat
import com.github.iryabov.invest.service.impl.profitPercentFormat
import net.n2oapp.criteria.dataset.DataSet

fun serializeAssets(assets: List<AssetView>): List<DataSet> {
    return assets.map { asset ->
        val map = DataSet()
        map["id"] = asset.assetTicker
        map["asset.ticker"] = asset.assetTicker
        map["name"] = asset.assetName
        map["quantity"] = asset.quantity
        map["netValue"] = asset.netValue
        map["marketValue"] = asset.marketValue
        map["valueProfitPercent"] = profitFormat(asset.valueProfitPercent)
        map["fixedProfitPercent"] = profitFormat(asset.fixedProfitPercent)
        map["marketProfitPercent"] = profitPercentFormat(asset.marketProfitPercent)
        map["marketProfit"] = asset.marketValue + asset.proceeds - asset.expenses
        map["marketProfitColor"] = color(asset.marketProfitPercent)
        map["netProportion"] = percentFormat(asset.netProportion)
        map["marketProportion"] = percentFormat(asset.marketProportion)
        map["marketProfitProportion"] = profitPercentFormat(asset.marketProfitProportion)
        map["targetProportion"] = percentFormat(asset.targetProportion)
        map["targetDeviationPercent"] = percentFormat(asset.targetDeviationPercent)
        map["targetDeviationColor"] = color(asset.targetDeviationPercent)
        map["assetActive"] = asset.quantity > 0
        map
    }
}

fun mapTargets(view: List<ChartView>): DataSet {
    val data = DataSet()
    view.forEach {
        data["data.${it.name}"] = it.value.toLong()
    }
    return data
}