package com.github.iryabov.invest.relation

enum class DealType(val income: Boolean = true, val quantity: Boolean = false, val currency: Boolean = false) {
    SALE(income = true, quantity = true, currency = false),
    PURCHASE(income = false, quantity = true, currency = false),
    DIVIDEND(income = true, quantity = false, currency = false),
    COUPON(income = true, quantity = false, currency = false),
    PERCENT(income = false, quantity = true, currency = false),
    TAX(income = true, quantity = false, currency = true),
    DEPOSIT(income = false, quantity = false, currency = true),
    WITHDRAWALS(income = true, quantity = false, currency = true);



    fun invert(): DealType {
        return when {
            this == SALE -> PURCHASE
            this == PURCHASE -> SALE
            this == WITHDRAWALS -> DEPOSIT
            this == DEPOSIT -> WITHDRAWALS
            else -> this
        }
    }
}