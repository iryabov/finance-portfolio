package com.github.iryabov.invest.entity

import com.github.iryabov.invest.relation.TargetType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal

@Table("target")
data class Target(
        @Id
        var id: Int? = null,
        @Column
        var active: Boolean = true,
        @Column
        val ticker: String,
        @Column
        val type: TargetType = TargetType.ASSET,
        @Column("portfolio_id")
        val portfolioId: Int,
        @Column
        var proportion: BigDecimal? = null,
        @Column("take_profit")
        val takeProfit: BigDecimal? = null,
        @Column("stop_loss")
        val stopLoss: BigDecimal? = null
)