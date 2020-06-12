package com.github.iryabov.invest.entity

import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.relation.DialType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate

@Table("dial")
class Dial(
        @Id
        var id: Long? = null,
        @Column
        val active: Boolean = true,
        @Column
        val ticker: String,
        @Column("account_id")
        val accountId: Int,
        @Column("dt_open")
        val dateOpen: LocalDate = LocalDate.now(),
        @Column
        val type: DialType = DialType.PURCHASE,
        @Column
        val currency: Currency = Currency.RUB,
        @Column
        val amount: BigDecimal = BigDecimal.ZERO,
        @Column
        val quantity: Int = 0,
        @Column
        val fee: BigDecimal = BigDecimal.ZERO,
        @Column("_sold_quantity")
        val soldQuantity: Int = 0,
        @Column("_sold_amount")
        val soldAmount: BigDecimal = BigDecimal.ZERO,
        @Column("dt_close")
        val dateClose: LocalDate? = null,
        @Column
        val tax: BigDecimal = BigDecimal.ZERO,
        @Column
        val note: String? = null
)