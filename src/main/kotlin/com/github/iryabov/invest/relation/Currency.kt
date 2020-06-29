package com.github.iryabov.invest.relation

enum class Currency {
    RUB,
    USD,
    EUR;

    fun tickerEquals(ticker: String) = name == ticker

}