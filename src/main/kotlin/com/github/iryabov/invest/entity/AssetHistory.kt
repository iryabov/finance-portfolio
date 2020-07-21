package com.github.iryabov.invest.entity

import com.github.iryabov.invest.relation.Currency
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate

@Table
data class AssetHistory(
        @Id
        var id: Long? = null,
        @Column("dt")
        val date: LocalDate,
        @Column("ticker")
        val ticker: String,
        @Column("price")
        val price: BigDecimal,
        @Column("currency")
        val currency: Currency = Currency.RUB
)