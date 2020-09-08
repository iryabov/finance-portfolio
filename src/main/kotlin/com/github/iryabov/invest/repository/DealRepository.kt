package com.github.iryabov.invest.repository

import com.github.iryabov.invest.entity.Deal
import com.github.iryabov.invest.model.AssetHistoryView
import com.github.iryabov.invest.model.AssetView
import com.github.iryabov.invest.model.DealView
import com.github.iryabov.invest.relation.Currency
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate


@Repository
interface DealRepository : CrudRepository<Deal, Long> {
    @Modifying
    @Query("UPDATE dial SET active = NOT active WHERE id = :id")
    fun deactivate(@Param("id") id: Long): Boolean

    @Query("""
    select 
        d.dt as date,
        ((case when d.currency = :currency then abs(d.volume)
         else coalesce((select r.price * abs(d.volume) from rate r where  r.dt = d.dt and  r.currency_purchase = d.currency and  r.currency_sale = :currency), 0) 
         end 
        ) / abs(d.quantity)) as price,
        d.quantity as quantity
    from dial d
    where (:account_id is null or d.account_id = :account_id)
      and d.active = true
      and d.ticker = :ticker 
      and d.type in ('PURCHASE', 'SALE') 
      and d.dt >= :from 
      and d.dt <= :till
    order by d.dt, d.id  
    """)
    fun findAllByPeriod(@Param("account_id") accountId: Int? = null,
                        @Param("ticker") ticker: String,
                        @Param("currency") currency: Currency,
                        @Param("from") from: LocalDate,
                        @Param("till") till: LocalDate): List<AssetHistoryView>

    @Query("""
select 
    a.asset_ticker,
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
    a.quantity,
    a.volume_cur,
    a.net_value,
    a.deposit,
    a.withdrawals,
    a.expenses,
    a.proceeds 
from (
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
        where d.account_id = :account_id
          and d.active = true
        union 
        select 
            d.id,
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
        where d.account_id = :account_id
          and d.active = true
          and d.ticker != d.currency
        ) d  
    where (:ticker is null or d.ticker = :ticker)
    group by d.ticker
) a
left join asset s on s.ticker = a.asset_ticker
    """)
    fun findAssets(@Param("account_id") accountId: Int,
                   @Param("currency") currency: Currency,
                   @Param("ticker") ticker: String? = null): List<AssetView>

    @Query("""
select 
    d.id,
    d.active as active,
    d.dt as dt,
    d.ticker as asset_ticker,
    a.name as asset_name,
    d.type as type,
    d.quantity as quantity,
    d.currency as currency,
    d.settlement_quantity as settlement_quantity,
    d.settlement_ticker as settlement_ticker,
    (case when d.currency = :currency then abs(d.volume)
     else abs(coalesce((select r.price * d.volume from rate r where  r.dt = d.dt and  r.currency_purchase = d.currency and  r.currency_sale = :currency), 0)) 
     end 
    ) as volume,
    (select sum(case when df.currency = :currency then df.volume 
                else coalesce((select r.price * df.volume from rate r where  r.dt = df.dt and  r.currency_purchase = df.currency and  r.currency_sale = :currency), 0)
                end / df.quantity * w.quantity
            ) 
     from writeoff w
     join dial df on df.id = w.dial_from and df.ticker = w.ticker
     where w.dial_to = d.id and w.ticker = d.ticker
    ) as sold_volume,
    (case when d.quantity <> 0 then abs(d.volume / d.quantity)
     else 0 
     end
    ) as price,
    (
        select sum(w.quantity) as sold_quantity
        from writeoff w
        where w.dial_from = d.id
          and d.quantity > 0
    ) as sold_quantity,
    (case when d.type = 'DIVIDEND' then
        (select sum(od.quantity)
        from dial od
        where od.account_id = :account_id
          and od.ticker = d.ticker 
          and od.active = true
          and od.dt < d.dt)
     else null end     
    ) as dividend_quantity
from (
    select  
        d.id,
        d.active as active,
        d.dt as dt,
        d.ticker as ticker,
        d.type as type,
        d.quantity as quantity,
        d.currency as currency,
        d.currency as settlement_ticker,
        d.volume as settlement_quantity,
        d.volume as volume
     from dial d
     where d.account_id = :account_id
       and (:ticker is null or d.ticker = :ticker)   
     union   
     select  
        d.id,
        d.active as active,
        d.dt as dt,
        d.currency as ticker,
        (case 
            when d.type = 'SALE' then 'PURCHASE'
            when d.type = 'PURCHASE' then 'SALE'
            else d.type
         end) as type,
        d.volume as quantity,
        (case 
         when d.ticker in ('RUB', 'USD', 'EUR') then d.ticker
         else d.currency
         end
        ) as currency,
        d.ticker as settlement_ticker,
        d.quantity as settlement_quantity,
        (case 
         when d.ticker in ('RUB', 'USD', 'EUR') then d.quantity
         else d.volume
         end
        ) as volume
     from dial d 
     where d.account_id = :account_id and d.currency = :ticker and d.ticker <> :ticker
) d
left join asset a on a.ticker = d.ticker
order by d.dt desc, d.id desc    
    """)
    fun findAllByAsset(@Param("account_id") accountId: Int,
                       @Param("currency") currency: Currency,
                       @Param("ticker") ticker: String?): List<DealView>

    @Query("""
        select *
        from dial d
        where d.active = true
          and d.account_id = :account_id
          and (d.dt > :date_from or (d.dt = :date_from and d.id > :id))
        order by d.dt, d.id
    """)
    fun findAllSaleAndPurchaseLaterThan(@Param("account_id") accountId: Int,
                                        @Param("ticker") ticker: String,
                                        @Param("date_from") dateFrom: LocalDate,
                                        @Param("id") dialId: Long): List<Deal>


    @Query("""
select 
    d.id,
    d.active as active,
    d.dt as dt,
    d.ticker as asset_ticker,
    a.name as asset_name,
    d.type as type,
    d.quantity as quantity,
    d.currency as currency,
    (case when d.currency = :currency then abs(d.volume)
     else abs(coalesce((select r.price * d.volume from rate r where  r.dt = d.dt and  r.currency_purchase = d.currency and  r.currency_sale = :currency), 0)) 
     end 
    ) as volume,
    (select sum(case when df.currency = :currency then df.volume 
                else coalesce((select r.price * df.volume from rate r where  r.dt = df.dt and  r.currency_purchase = df.currency and  r.currency_sale = :currency), 0)
                end / df.quantity * w.quantity
            ) 
     from writeoff w
     join dial df on df.id = w.dial_from and df.ticker = w.ticker
     where w.dial_to = d.id and w.ticker = d.ticker
    ) as sold_volume,
    (case when d.quantity <> 0 then abs(d.volume / d.quantity)
     else 0 
     end
    ) as price,
    (
        select sum(w.quantity) as sold_quantity
        from writeoff w
        where w.dial_from = d.id
          and d.quantity > 0
    ) as sold_quantity,
    (case when d.type = 'DIVIDEND' then
        (select sum(od.quantity)
        from dial od
        where od.account_id = :account_id
          and od.ticker = d.ticker 
          and od.active = true
          and od.dt < d.dt)
     else null end     
    ) as dividend_quantity
from dial d
left join asset a on a.ticker = d.ticker
where d.account_id = :account_id
  and (:ticker is null or d.ticker = :ticker)   
order by d.dt desc, d.id desc   
    """)
    fun findAllByCurrency(@Param("account_id") accountId: Int,
                          @Param("currency") settlementCurrency: Currency,
                          @Param("ticker") currency: Currency?): List<DealView>
}