package com.github.iryabov.invest.repository

import com.github.iryabov.invest.entity.Target
import com.github.iryabov.invest.model.AssetView
import com.github.iryabov.invest.relation.Currency
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface TargetRepository: CrudRepository<Target, Long> {
    @Query("""
select 
    t.active,
    t.ticker as asset_ticker,
    s.name as asset_name,
    s.class as asset_class,
    (case when s.currency = :currency then s.price_now
     else coalesce(
        (select r.price
         from rate r 
         where r.currency_purchase = s.currency 
           and r.currency_sale = :currency
           and r.dt <= current_date
         order by r.dt desc limit 1) * s.price_now, 
        null) 
     end) as asset_price_now,
    t.proportion,
    t.take_profit,
    t.stop_loss,
    t.note,
    a.quantity,
    a.volume_cur,
    a.net_value,
    a.deposit,
    a.withdrawals,
    a.expenses,
    a.proceeds 
from target t
left join asset s on s.ticker = t.ticker
left join (
    select 
        a.ticker,
        a.quantity,
        a.volume_cur,
        a.net_value,
        a.deposit,
        a.withdrawals,
        a.expenses,
        a.proceeds 
    from (
        select 
            s.ticker as ticker,
            0 as quantity,
            0 as volume_cur,
            0 as net_value,
            0 as deposit,
            0 as withdrawals,
            0 as expenses,
            0 as proceeds
        from asset s     
    ) a
) a on a.ticker = t.ticker
where t.portfolio_id = :portfolio_id
    """)
    fun findAllViews(@Param("portfolio_id") portfolioId: Int,
                     @Param("currency") currency: Currency): List<AssetView>
}