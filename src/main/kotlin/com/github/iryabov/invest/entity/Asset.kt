package com.github.iryabov.invest.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table
data class Asset(
        @Id
        val ticker: String,
        @Column
        val name: String,
        @Column
        val type: String? = null,
        @Column
        val sector: String? = null,
        @Column
        val country: String? = null,
        @Column
        val currency: String? = null,
        @Column("price_now")
        val priceNow: String? = null,
        @Column("price_week")
        val priceWeek: String? = null,
        @Column("price_month")
        val priceMonth: String? = null,
        @Column("price_year")
        val priceYear: String? = null
)