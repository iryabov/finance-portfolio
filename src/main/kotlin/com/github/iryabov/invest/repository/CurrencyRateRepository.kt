package com.github.iryabov.invest.repository

import com.github.iryabov.invest.entity.CurrencyPair
import com.github.iryabov.invest.relation.Currency
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface CurrencyRateRepository : CrudRepository<CurrencyPair, Long> {
    @Query("""
            select r.*
            from rate r
            where r.dt <= :dt
              and r.currency_purchase = :base
            order by r.dt desc
            limit 1
            """)
    fun findByDateAndBase(@Param("dt") date: LocalDate,
                          @Param("base") base: Currency): List<CurrencyPair>

    @Query("""
        select r.*
        from rate r
        where r.currency_purchase = :pair1 
          and r.currency_sale = :pair2
          and r.dt >= :from
          and r.dt <= :till
    """)
    fun findAllByPair(@Param("pair1") pair1: Currency,
                      @Param("pair2") pair2: Currency,
                      @Param("from") from: LocalDate,
                      @Param("till") till: LocalDate): List<CurrencyPair>
}