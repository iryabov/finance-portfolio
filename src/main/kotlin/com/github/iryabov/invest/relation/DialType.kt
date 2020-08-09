package com.github.iryabov.invest.relation

import java.lang.IllegalArgumentException

enum class DialType(val income: Boolean = true, val quantity: Boolean = false, val currency: Boolean = false) {
    SALE(income = true, quantity = true, currency = false),
    PURCHASE(income = false, quantity = true, currency = false),
    DIVIDEND(income = true, quantity = false, currency = false),
    COUPON(income = true, quantity = false, currency = false),
    PERCENT(income = true, quantity = true, currency = false),
    TAX(income = false, quantity = false, currency = false),
    DEPOSIT(income = false, quantity = false, currency = true),
    WITHDRAWALS(income = true, quantity = false, currency = true);



    fun invert(): DialType {
        return when {
            this == SALE -> PURCHASE
            this == PURCHASE -> SALE
            this == WITHDRAWALS -> DEPOSIT
            this == DEPOSIT -> WITHDRAWALS
            this == DIVIDEND -> DIVIDEND
            this == TAX -> TAX
            else -> throw IllegalArgumentException("$this is not inverted")
        }
    }
}