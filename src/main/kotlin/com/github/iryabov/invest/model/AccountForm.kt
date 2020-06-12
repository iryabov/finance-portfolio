package com.github.iryabov.invest.model

import com.github.iryabov.invest.entity.Account

data class AccountForm(
        val name: String,
        val num: String?
)

fun AccountForm.toEntity() = Account(
        name = name,
        num = num
)