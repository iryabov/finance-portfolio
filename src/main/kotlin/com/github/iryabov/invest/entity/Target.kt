package com.github.iryabov.invest.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal

@Table("target")
data class Target(
    @Id
    var id: Long? = null,
    @Column
    var active: Boolean = true,
    @Column
    val ticker: String,
    @Column("portfolio_id")
    val portfolioId: Int,
    @Column
    val proportion: BigDecimal? = null,
    @Column("take_profit")
    val takeProfit: BigDecimal? = null,
    @Column("stop_loss")
    val stopLoss: BigDecimal? = null
)