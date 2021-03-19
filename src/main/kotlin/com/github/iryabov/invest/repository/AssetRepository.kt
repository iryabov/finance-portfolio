package com.github.iryabov.invest.repository

import com.github.iryabov.invest.entity.Asset
import com.github.iryabov.invest.relation.AssetClass
import com.github.iryabov.invest.relation.Country
import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.relation.Sector
import org.springframework.data.domain.Sort
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AssetRepository: PagingAndSortingRepository<Asset, String> {

    @Query("""
select *
from asset s                
where not exists (select t.id from target t where t.portfolio_id = :portfolio_id and t.ticker = s.ticker and t.type = 'ASSET')
  and (:class is null or s.class = :class)
  and (:sector is null or s.sector = :sector)
  and (:country is null or s.country = :country)
  and (:currency is null or s.currency = :currency)
  and (:account_id is null or exists (select d.id from dial d where d.active = true and d.ticker = s.ticker and d.account_id = :account_id))
  and exists (select s.ticker from portfolio_account pa join dial d on d.account_id = pa.account_id where pa.portfolio_id = :portfolio_id and d.ticker = s.ticker)
"""
    )
    fun findAllCandidates(@Param("portfolio_id") portfolioId: Int,
                          @Param("class") assetClass: AssetClass? = null,
                          @Param("sector") sector: Sector? = null,
                          @Param("country") country: Country? = null,
                          @Param("currency") currency: Currency? = null,
                          @Param("account_id") accountId: Int? = null): List<Asset>

    @Query("""
select *
from asset s  
where s.ticker like '%'||:name||'%' or s.name like '%'||:name||'%'
    """)
    fun findAllByNameOrTicker(@Param("name") name: String, sort: Sort): List<Asset>
}