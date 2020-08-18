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

fun color(profit: BigDecimal?, dividendProfit: BigDecimal?): String {
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

fun round(profit: BigDecimal?, dividendProfit: BigDecimal?): BigDecimal? {
    return if (profit != null && profit.notZero())
        profit.round(0)
    else if (dividendProfit != null && dividendProfit.notZero())
        dividendProfit.round(0)
    else
        null
}