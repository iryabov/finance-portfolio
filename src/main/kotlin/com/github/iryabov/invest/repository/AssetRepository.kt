package com.github.iryabov.invest.repository

import com.github.iryabov.invest.entity.Asset
import com.github.iryabov.invest.model.SecurityView
import com.github.iryabov.invest.relation.Currency
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface AssetRepository: CrudRepository<Asset, String> {
    @Query("""
    select 
        a.ticker as ticker,
        a.name as name,
        a.class as asset_class,
        a.sector as asset_sector,
        a.country as asset_country
    from asset a
    where a.ticker = :ticker
    """)
    fun findSecurityByTicker(@Param("ticker") ticker: String): SecurityView
}