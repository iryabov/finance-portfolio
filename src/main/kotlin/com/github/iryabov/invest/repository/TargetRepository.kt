package com.github.iryabov.invest.repository

import com.github.iryabov.invest.entity.Target
import com.github.iryabov.invest.model.*
import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.relation.TargetType
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface TargetRepository : CrudRepository<Target, Long> {
    @Query("""
select 
    t.active,
    t.ticker as asset_ticker,
    s.name as asset_name,
    s.class as asset_class,
    s.sector as asset_sector,
    s.country as asset_country,
    s.currency as asset_currency,
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
    t.proportion as target_proportion,
    t.take_profit,
    t.stop_loss,
    t.note,
    coalesce(a.quantity, 0) as quantity,
    coalesce(a.volume_cur, 0) as volume_cur,
    coalesce(a.net_value, 0) as net_value,
    coalesce(a.deposit, 0) as deposit,
    coalesce(a.withdrawals, 0) as withdrawals,
    coalesce(a.expenses, 0) as expenses,
    coalesce(a.proceeds, 0) as proceeds
from target t
left join asset s on s.ticker = t.ticker
left join (
    select 
        d.ticker as asset_ticker,
        sum(d.quantity) as quantity,
        sum(volume_cur) as volume_cur,
        sum(case when d.quantity > 0 
            then  -1 * (d.volume_cur * (d.quantity - d.sold_quantity) / d.quantity)
            else  0 
            end  
        ) as net_value,
        sum(case  
          when  d.type = 'DEPOSIT' then  -1*d.volume_cur
          else  0 
           end) as deposit,
        sum(case  
           when  d.type = 'WITHDRAWALS' then  d.volume_cur
           else   0 
           end) as withdrawals,
        sum(case 
           when  d.volume_cur < 0 then  -1*d.volume_cur
           else  0 
           end) as expenses,
        sum(case 
           when  d.volume_cur > 0 then  d.volume_cur
           else   0 
           end) as proceeds 
    from
        (select 
            d.id,
            d.account_id as account_id,
            d.ticker as ticker,
            d.dt as dt,
            (case when active = true then d.quantity else 0 end) as quantity,
            d.currency as currency,
            (case when active = true then d.volume else 0 end) as volume,
            d.type as type,
            (case when active = true then 
            (case when d.currency = :currency then  d.volume
             else coalesce((select r.price * d.volume from rate r where  r.dt = d.dt and  r.currency_purchase = d.currency and  r.currency_sale = :currency), 0) 
             end 
            ) else 0 end) as volume_cur,
            (case when d.quantity > 0 
             then (
                 select coalesce(sum(w.quantity),0) as sold_quantity
                 FROM writeoff w
                 JOIN dial d2 ON d2.id = w.dial_to
                 WHERE w.dial_from = d.id
             )
             else 0 end
            ) as sold_quantity
        from dial d
        where d.active = true
        union 
        select 
            d.id,
            d.account_id as account_id,
            d.currency as ticker,
            d.dt as dt,
            d.volume as quantity,
            d.ticker as currency,
            d.quantity as volume,
            (case 
                when d.type = 'SALE' then 'PURCHASE'
                when d.type = 'PURCHASE' then 'SALE'
                else d.type
             end) as type,
            (case when d.currency = :currency then -1*d.volume
             else -1*coalesce((select r.price * d.volume from rate r where  r.dt = d.dt and  r.currency_purchase = d.currency and  r.currency_sale = :currency), 0) 
             end 
            ) as volume_cur,
            (case when d.volume > 0 
             then (
                 select coalesce(sum(w.quantity),0) as sold_quantity
                 FROM writeoff w
                 JOIN dial d2 ON d2.id = w.dial_to
                 WHERE w.dial_from = d.id
             )
             else 0 end
            ) as sold_quantity
        from dial d
        where d.active = true
          and d.ticker != d.currency
        ) d  
    where (:ticker is null or d.ticker = :ticker)
      and exists (select account_id from portfolio_account pa where pa.portfolio_id = :portfolio_id and pa.account_id = d.account_id)
    group by d.ticker   
) a on a.asset_ticker = t.ticker
where t.portfolio_id = :portfolio_id
  and t.type = 'ASSET'
  and (:ticker is null or t.ticker = :ticker)
    """)
    fun findAllAssetsViews(@Param("portfolio_id") portfolioId: Int,
                           @Param("currency") currency: Currency,
                           @Param("ticker") ticker: String? = null): List<AssetView>

    fun findByPortfolioIdAndTicker(portfolioId: Int,
                                   ticker: String): Optional<Target>

    fun findByPortfolioIdAndTickerAndType(portfolioId: Int,
                                          ticker: String,
                                          type: TargetType): Optional<Target>

    fun findAllByPortfolioIdAndType(portfolioId: Int, type: TargetType): List<Target>

    fun findAllByPortfolioId(portfolioId: Int): List<Target>


    @Query("""
select
  d.dt as date,
  sum(-1*d.deposit) as deposit,
  sum(d.withdrawals) as withdrawals,
  sum(coalesce(d.quantity * price_cur, -1*d.volume)) as market_value,
  sum(-1*d.volume) as balance,
  sum(d.profit) as profit_value,
  sum(d.quantity) as quantity,
  sum(d.purchase_volume / d.purchase_quantity * d.quantity) as net_value,
  sum(d.sold_volume - d.purchase_volume / d.purchase_quantity * d.sold_quantity) as trading,
  sum(d.dividends) as dividends,
  sum(d.coupons) as coupons,
  sum(d.percents) as percents,
  sum(d.taxes) as taxes
from
(select 
  p as dt, 
  d.ticker,
  sum(d.quantity) as quantity,
  sum(case when d.quantity > 0 then d.quantity else 0 end) as purchase_quantity,
  sum(case when d.quantity < 0 then -1*d.quantity else 0 end) as sold_quantity,
  sum(case when d.quantity > 0 then -1*d.volume_cur else 0 end) as purchase_volume,
  sum(case when d.quantity < 0 then d.volume_cur else 0 end) as sold_volume,
  sum(d.volume_cur) as volume,
  sum(case when d.type = 'DEPOSIT' then d.volume_cur else 0 end) as deposit,
  sum(case when d.type = 'WITHDRAWALS' then d.volume_cur else 0 end) as withdrawals,
  sum(case when d.type = 'DIVIDEND' and d.quantity = 0 then d.volume_cur else 0 end) as dividends,
  sum(case when d.type = 'COUPON' and d.quantity = 0 then d.volume_cur else 0 end) as coupons,
  sum(case when d.type = 'PERCENT' then d.volume_cur else 0 end) as percents,
  sum(case when d.type = 'TAX' then d.volume_cur else 0 end) as taxes,
  sum(case when d.quantity = 0 then d.volume_cur else 0 end) as profit,
  (select h.price * exchange(:currency, s.currency, h.dt)
    from asset_history h 
    join asset s on s.ticker = h.ticker
    where h.ticker = d.ticker 
      and h.dt <= p
    order by h.dt desc
    limit 1) as price_cur
from generate_series(:from, :till, interval '1 week') as p
cross join (
    select 
        d.id,
        d.account_id as account_id,
        d.ticker as ticker,
        d.dt as dt,
        d.type,
        d.quantity as quantity,
        d.volume * exchange(:currency, d.currency, d.dt) as volume_cur
    from dial d
    where d.active = true
    union 
    select 
        d.id,
        d.account_id as account_id,
        d.currency as ticker,
        d.dt as dt,
        (case 
            when d.type = 'SALE' then 'PURCHASE'
            when d.type = 'PURCHASE' then 'SALE'
            else d.type
        end) as type,
        d.volume as quantity,
        (-1 * d.volume * exchange(:currency, d.currency, d.dt)) as volume_cur
    from dial d
    where d.active = true
      and d.ticker != d.currency
) d
join target t on t.ticker = d.ticker and t.type = 'ASSET'
where t.portfolio_id = :portfolio_id
  and exists (select * from portfolio_account pa where pa.portfolio_id = :portfolio_id and pa.account_id = d.account_id)
  and d.dt <= p
group by p, d.ticker) d
group by d.dt
    """)
    fun findAllTargetHistoryViews(@Param("portfolio_id") portfolioId: Int,
                                  @Param("currency") currency: Currency,
                                  @Param("from") from: LocalDate?,
                                  @Param("till") till: LocalDate?,
                                  @Param("step") step: String): List<TargetHistoryView>

    @Query("update target set proportion = null where portfolio_id = :portfolio_id and type = :type")
    @Modifying
    fun updateProportionByPortfolioIdAndType(@Param("portfolio_id") portfolioId: Int,
                                      @Param("type") type: TargetType)

}