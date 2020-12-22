package com.github.iryabov.invest.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate

@Table("portfolio")
data class Portfolio(
        @Id
        var id: Int? = null,
        @Column
        val name: String,
        @Column
        val note: String? = null,
        @Column
        val beginDate: LocalDate? = null,
        @Column
        val endDate: LocalDate? = null
)