package com.github.iryabov.invest.ui

import com.github.iryabov.invest.model.AssetView
import com.github.iryabov.invest.model.ChartView
import com.github.iryabov.invest.model.RefForm
import com.github.iryabov.invest.service.impl.*
import net.n2oapp.criteria.dataset.DataSet

fun serializeAssets(assets: List<AssetView>): List<DataSet> {
    return assets.map { asset ->
        val map = DataSet()
        map["id"] = asset.assetTicker
        map["asset.ticker"] = asset.assetTicker
        map["name"] = fullName(asset.assetName, asset.assetTicker)
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

fun sumData(data: Map<String, Int>): Int {
    return data.values.sumBy { it }
}

fun mapToRef(data: DataSet) = RefForm(id = data.getInteger("id"), name = data.getString("name"))

fun mapListToRef(list: List<DataSet>) = list.map { mapToRef(it) }

fun mapToData(ref: RefForm) = DataSet(mapOf("id" to ref.id, "name" to ref.name))

fun mapListToData(list: List<RefForm>) = list.map { mapToData(it) }