package com.github.iryabov.invest.model

import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.relation.DialType
import com.github.iryabov.invest.service.impl.P0
import java.math.BigDecimal
import java.time.LocalDate

data class DialForm(
        var ticker: String,
        var opened: LocalDate? = null,
        var type: DialType,
        var currency: Currency,
        var volume: BigDecimal,
        var quantity: Int
) {
    constructor() : this("", null, DialType.PURCHASE, Currency.RUB, P0, 0)
}