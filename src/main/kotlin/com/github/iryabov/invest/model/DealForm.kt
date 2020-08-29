package com.github.iryabov.invest.model

import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.relation.DealType
import com.github.iryabov.invest.service.impl.P0
import java.math.BigDecimal
import java.time.LocalDate

data class DealForm(
        var ticker: String,
        var opened: LocalDate? = null,
        var type: DealType,
        var currency: Currency,
        var volume: BigDecimal,
        var quantity: Int? = null,
        var remittanceAccountId: Int? = null
) {
    constructor() : this("", null, DealType.PURCHASE, Currency.RUB, P0, 0)
}