package com.github.iryabov.invest.model

import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.relation.DialType
import java.math.BigDecimal
import java.time.LocalDate

data class DialForm(
        val ticker: String,
        val opened: LocalDate? = null,
        val type: DialType,
        val currency: Currency,
        val amount: BigDecimal,
        val quantity: Int
)