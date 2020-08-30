package com.github.iryabov.invest.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.iryabov.invest.relation.*
import com.github.iryabov.invest.service.impl.P0
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate

@Table
data class Asset (
        @Id
        var ticker: String,
        @Column
        var name: String,
        @Column("class")
        var assetClass: AssetClass? = null,
        @Column
        var sector: Sector? = null,
        @Column
        var country: Country? = null,
        @Column
        var currency: Currency? = null,
        @Column
        var api: FinanceApi = FinanceApi.MOEX,
        @Column("price_now")
        var priceNow: BigDecimal? = null,
        @Column("price_week")
        var priceWeek: BigDecimal? = null,
        @Column("price_month")
        var priceMonth: BigDecimal? = null,
        @Column("last_update")
        var lastUpdate: LocalDate? = null
): Persistable<String> {

        constructor(): this("", "")

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