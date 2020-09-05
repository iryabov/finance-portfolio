package com.github.iryabov.invest.model

/**
 * Итоги по всем счетам
 */
data class TotalView(
        val accounts: List<AccountView>
): ValueView()