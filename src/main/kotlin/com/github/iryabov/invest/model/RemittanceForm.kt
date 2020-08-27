package com.github.iryabov.invest.model

import com.github.iryabov.invest.relation.Currency
import java.time.LocalDate

data class RemittanceForm(
        var opened: LocalDate,
        var currency: Currency,
        var quantity: Int
        ) {
    constructor(): this(opened = LocalDate.now(), currency = Currency.RUB, quantity = 0)
}