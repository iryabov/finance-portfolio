package com.github.iryabov.invest.model

data class Balance (
        val dialFrom: Long,
        val purchasedQuantity: Int,
        val soldQuantity: Int = 0
) {
    val balancedQuantity: Int
        get() = purchasedQuantity - soldQuantity
}