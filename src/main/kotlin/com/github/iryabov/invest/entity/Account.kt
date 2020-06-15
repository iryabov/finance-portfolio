package com.github.iryabov.invest.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("account")
data class Account(
        @Id
        var id: Int? = null,
        @Column
        val name: String,
        @Column
        val num: String? = null
)