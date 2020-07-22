package com.github.iryabov.invest.repository

import com.github.iryabov.invest.entity.AssetHistory
import com.github.iryabov.invest.model.HistoryView
import com.github.iryabov.invest.relation.Currency
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface AssetHistoryRepository : CrudRepository<AssetHistory, Long> {
    fun findByTickerAndDate(ticker: String, date: LocalDate): AssetHistory

    @Query(
    """
    select h.dt as date,
           (case when h.currency = :currency then h.price
            else coalesce(r.price * h.price, 0) 
           end) as price,
    from asset_history h
    left join rate r on r.dt = h.dt and r.currency_purchase = h.currency and r.currency_sale = :currency
    where h.ticker = :ticker
      and h.dt >= :from
      and h.dt <= :till
    """)
    fun findAllHistoryByTicker(@Param("ticker") ticker: String,
                               @Param("from") from: LocalDate,
                               @Param("till") till: LocalDate,
                               @Param("currency") currency: Currency): List<HistoryView>
}