package com.github.iryabov.invest.relation

import java.lang.IllegalArgumentException

enum class DialType(val income: Boolean = true, val quantity: Boolean = false, val currency: Boolean = false) {
    SALE(quantity = true),
    PURCHASE(income = false, quantity = true),
    DIVIDEND,
    TAX(false),
    DEPOSIT(income = false, currency = true),
    WITHDRAWALS(currency = true);



    fun invert(): DialType {
        return when {
            this == SALE -> PURCHASE
            this == PURCHASE -> SALE
            this == WITHDRAWALS -> DEPOSIT
            this == DEPOSIT -> WITHDRAWALS
            else -> throw IllegalArgumentException("$this is not inverted")
        }
    }
}