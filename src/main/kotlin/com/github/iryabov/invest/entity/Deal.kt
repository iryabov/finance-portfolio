package com.github.iryabov.invest.entity

import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.relation.DealType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate

@Table("dial")
data class Deal(
        @Id
        var id: Long? = null,
        @Column
        val active: Boolean = true,
        @Column
        val ticker: String,
        @Column("account_id")
        val accountId: Int,
        @Column("dt")
        val date: LocalDate = LocalDate.now(),
        @Column
        val type: DealType = DealType.PURCHASE,
        @Column
        val currency: Currency? = Currency.RUB,
        @Column
        val volume: BigDecimal = BigDecimal.ZERO,
        @Column
        val quantity: Int = 0,
        @Column
        val fee: BigDecimal = BigDecimal.ZERO,
        @Column
        val tax: BigDecimal = BigDecimal.ZERO,
        @Column
        val note: String? = null
)