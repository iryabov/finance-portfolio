package com.github.iryabov.invest.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.iryabov.invest.relation.AssetClass
import com.github.iryabov.invest.relation.Country
import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.relation.Sector
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal

@Table
data class Asset (
        @Id
        val ticker: String,
        @Column
        val name: String,
        @Column("class")
        val assetClass: AssetClass? = null,
        @Column
        val sector: Sector? = null,
        @Column
        val country: Country? = null,
        @Column
        val currency: Currency? = null,
        @Column("price_now")
        val priceNow: BigDecimal? = null,
        @Column("price_week")
        val priceWeek: BigDecimal? = null,
        @Column("price_month")
        val priceMonth: BigDecimal? = null
): Persistable<String> {
        @org.springframework.data.annotation.Transient
        @JsonIgnore
        var newEntity = false

        override fun getId(): String? {
                return ticker
        }

        override fun isNew(): Boolean {
                return newEntity
        }
}