package com.github.iryabov.invest.repository

import com.github.iryabov.invest.entity.Portfolio
import com.github.iryabov.invest.model.PortfolioView
import com.github.iryabov.invest.relation.Currency
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PortfolioRepository: CrudRepository<Portfolio, Int> {

    @Query("""
        select p.id,
            p.name
        from portfolio p
    """)
    fun findAllView(currency: Currency): List<PortfolioView>
}