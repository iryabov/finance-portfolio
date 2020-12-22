package com.github.iryabov.invest.model

import java.time.LocalDate

data class PortfolioForm(
        var id: Int? = null,
        var name: String,
        var note: String? = null,
        var beginDate: LocalDate? = null,
        var endDate: LocalDate? = null
) {
    constructor() : this(name = "")
}