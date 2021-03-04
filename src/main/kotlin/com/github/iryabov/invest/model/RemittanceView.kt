package com.github.iryabov.invest.model

import com.github.iryabov.invest.relation.Currency
import java.time.LocalDate

data class RemittanceView(
        val accountFrom: String?,
        val accountTo: String?,
        val opened: LocalDate,
        val currency: Currency,
        val quantity: Int
)