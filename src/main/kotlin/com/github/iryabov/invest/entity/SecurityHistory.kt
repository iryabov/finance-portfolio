package com.github.iryabov.invest.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate

@Table("asset_history")
data class SecurityHistory(
        @Id
        var id: Long? = null,
        @Column("dt")
        val date: LocalDate,
        @Column("ticker")
        val ticker: String,
        @Column("price")
        val price: BigDecimal
)