package com.github.iryabov.invest.repository

import com.github.iryabov.invest.entity.SecurityHistory
import com.github.iryabov.invest.model.SecuritiesHistoryView
import com.github.iryabov.invest.model.SecurityHistoryView
import com.github.iryabov.invest.relation.Currency
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface SecurityHistoryRepository : CrudRepository<SecurityHistory, Long> {
    fun findByTickerAndDate(ticker: String, date: LocalDate): SecurityHistory?

    @Query(
            """
    select h.dt as date,
           (case when a.currency = :currency then h.price
            else coalesce(r.price * h.price, 0) 
           end) as price
    from asset_history h
    join asset a on a.ticker = h.ticker
    left join rate r on r.dt = h.dt and r.currency_purchase = a.currency and r.currency_sale = :currency
    where h.ticker = :ticker
      and h.dt >= :from
      and h.dt <= :till
    order by h.dt  
    """)
    fun findAllHistoryByTicker(@Param("ticker") ticker: String,
                               @Param("from") from: LocalDate,
                               @Param("till") till: LocalDate,
                               @Param("currency") currency: Currency): List<SecurityHistoryView>

    @Query("""
    delete from asset_history
     where ticker = :ticker 
       and dt >= :from and dt <= :till
    """)
    fun deleteByTicker(@Param("ticker") ticker: String,
                       @Param("from") from: LocalDate,
                       @Param("till") till: LocalDate)

    @Query("""
        select h.dt as date
        from asset_history h
        order by h.dt desc
        limit 1
    """)
    fun findLastDateByTicker(@Param("ticker") ticker: String): LocalDate?


    @Query("""
select 
    h.dt, 
    a.ticker, 
    (case when a.currency = :currency then h.price
     else coalesce(r.price * h.price, 0) 
     end) as price
from asset_history h
join asset a on a.ticker = h.ticker
join target t on t.ticker = a.ticker
left join rate r on r.dt = h.dt and r.currency_purchase = a.currency and r.currency_sale = :currency
where t.type = 'ASSET'
  and t.portfolio_id = :portfolio_id
  and (:from is null or h.dt >= :from) and (:till is null or h.dt <= :till) 
order by h.dt
    """)
    fun findAllHistoryByPortfolioId(@Param("portfolio_id") portfolioId: Int,
                                    @Param("currency") currency: Currency,
                                    @Param("from") from: LocalDate?,
                                    @Param("till") till: LocalDate?): List<SecuritiesHistoryView>
}