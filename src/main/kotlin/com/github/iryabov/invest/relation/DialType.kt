package com.github.iryabov.invest.relation

import java.lang.IllegalArgumentException

enum class DialType(val income: Boolean = true) {
    SALE,
    PURCHASE(false),
    DIVIDEND,
    TAX(false),
    DEPOSIT(false),
    WITHDRAWALS;



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