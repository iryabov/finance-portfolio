package com.github.iryabov.invest.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("writeoff")
data class WriteOff (
        @Id
        var id: Long? = null,
        @Column("dial_from")
        val dialFrom: Long,
        @Column("dial_to")
        val dialTo: Long,
        @Column
        val quantity: Int = 0
)