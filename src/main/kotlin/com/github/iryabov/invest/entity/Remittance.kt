package com.github.iryabov.invest.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("remittance")
data class Remittance (
        @Id
        var id: Long? = null,
        @Column("dial_from")
        var dialFrom: Long,
        @Column("dial_to")
        var dialTo: Long
)