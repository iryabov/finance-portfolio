package com.github.iryabov.invest.model

import java.math.BigDecimal

data class TargetForm(
        var targetProportion: Int? = null,
        var takeProfit: BigDecimal? = null,
        var stopLoss: BigDecimal? = null
) {
    constructor(): this(targetProportion = null)
}