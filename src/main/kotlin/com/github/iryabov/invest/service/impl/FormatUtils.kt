package com.github.iryabov.invest.service.impl

import java.math.BigDecimal

fun fullName(name: String?, code: String?): String {
    return if (name != null && name.isNotEmpty() && code != null && code.isNotEmpty())
        "$name ($code)"
    else if (name != null && name.isNotEmpty())
        "$name"
    else
        "$code"
}

@JvmOverloads
fun color(profit: BigDecimal?, dividendProfit: BigDecimal? = null): String {
    return if (profit != null && profit.notZero())
        if (profit.greater(P0))
            "success"
        else
            "danger"
    else
        if (dividendProfit != null && dividendProfit.greater(P0))
            "info"
        else
            ""
}

@JvmOverloads
fun round(profit: BigDecimal?, dividendProfit: BigDecimal? = null): String? {
    return if (profit != null && profit.notZero())
        profitFormat(profit)
    else if (dividendProfit != null && dividendProfit.notZero())
        profitFormat(dividendProfit)
    else
        null
}

@JvmOverloads
fun profitFormat(value: BigDecimal?, scale: Int = 0): String? {
    if (value == null || value.isZero()) return null
    return if (value.greater(P0))
        "+" + value.round(scale)
    else
        "" + value.round(scale)
}

@JvmOverloads
fun percentFormat(value: BigDecimal?, scale: Int = 0): String? {
    if (value == null || value.isZero()) return null
    return if (value.greater(P0))
        "+" + value.round(scale) + "%"
    else
        "" + value.round(scale) + "%"
}