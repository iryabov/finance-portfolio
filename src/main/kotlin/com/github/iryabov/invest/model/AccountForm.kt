package com.github.iryabov.invest.model

data class AccountForm(
        var name: String,
        var num: String? = null
) {

    constructor() : this("")

}