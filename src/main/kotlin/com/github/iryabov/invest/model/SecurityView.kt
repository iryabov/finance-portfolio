package com.github.iryabov.invest.model

import com.github.iryabov.invest.relation.AssetClass
import com.github.iryabov.invest.relation.Country
import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.relation.Sector
import com.github.iryabov.invest.service.impl.P0
import java.math.BigDecimal
import java.time.LocalDate

data class SecurityView(
        val ticker: String,
        val name: String,
        val priceNow: BigDecimal = P0,
        val priceWeek: BigDecimal = P0,
        val priceMonth: BigDecimal = P0,
        val priceQuarter: BigDecimal = P0,
        val priceYear: BigDecimal = P0,
        val assetClass: AssetClass?,
        val assetSector: Sector?,
        val assetCountry: Country?,
        var currency: Currency = Currency.RUB,
        var history: List<HistoryView>)

data class HistoryView(
        val date: LocalDate,
        val price: BigDecimal
)