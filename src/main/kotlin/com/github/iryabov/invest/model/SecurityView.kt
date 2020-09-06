package com.github.iryabov.invest.model

import com.github.iryabov.invest.relation.*
import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.service.impl.P0
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

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
        val api: FinanceApi = FinanceApi.CACHE,
        var currency: Currency = Currency.RUB
        ) {
    var history: List<SecurityHistoryView> = Collections.emptyList()
}

data class SecurityHistoryView(
        val date: LocalDate,
        val price: BigDecimal
)