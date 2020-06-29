package com.github.iryabov.invest.relation

import java.lang.IllegalArgumentException

enum class DialType {
    SALE,
    PURCHASE,
    DIVIDEND,
    TAX,
    DEPOSIT,
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