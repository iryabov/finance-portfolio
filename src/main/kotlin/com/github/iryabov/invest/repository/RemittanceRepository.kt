package com.github.iryabov.invest.repository

import com.github.iryabov.invest.entity.Remittance
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface RemittanceRepository: CrudRepository<Remittance, Long> {
    @Query("""
        select *
        from remittance
        where dial_from = :dial_id or dial_to = :dial_id
    """)
    fun findOneByDialFromOrTo(@Param("dial_id") dialId: Long): Remittance?
}