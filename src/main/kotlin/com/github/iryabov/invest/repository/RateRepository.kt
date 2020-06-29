package com.github.iryabov.invest.repository

import com.github.iryabov.invest.entity.CurrencyPair
import com.github.iryabov.invest.relation.Currency
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface RateRepository : CrudRepository<CurrencyPair, Long> {
    @Query("""
            select r.*
            from rate r
            where r.dt = :dt
              and r.currency_purchase = :base
            """)
    fun findByDateAndBase(@Param("dt") date: LocalDate,
                          @Param("base") base: Currency): List<CurrencyPair>
}