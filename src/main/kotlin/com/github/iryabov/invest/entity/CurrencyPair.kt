package com.github.iryabov.invest.entity

import com.github.iryabov.invest.relation.Currency
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate

@Table("rate")
data class CurrencyPair(
        @Id
        var id: Long? = null,
        @Column("dt")
        val date: LocalDate,
        @Column("currency_purchase")
        val currencyPurchased: Currency,
        @Column("currency_sale")
        val currencySale: Currency,
        @Column
        val price: BigDecimal = BigDecimal.ZERO
)