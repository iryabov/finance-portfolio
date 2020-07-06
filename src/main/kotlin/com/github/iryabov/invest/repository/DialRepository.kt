package com.github.iryabov.invest.repository

import com.github.iryabov.invest.entity.Dial
import com.github.iryabov.invest.model.AssetView
import com.github.iryabov.invest.model.Balance
import com.github.iryabov.invest.relation.Currency
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate


@Repository
interface DialRepository : CrudRepository<Dial, Long> {
    @Modifying
    @Query("UPDATE dial SET active = NOT active WHERE id = :id")
    fun deactivate(@Param("id") id: Long): Boolean

    @Query("""
select 
    d.ticker as asset_ticker,
    sum(d.quantity) as quantity,
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
    d.quantity as quantity,
    d.currency as currency,
    d.volume as volume,
    d.type as type,
    (case when d.currency = :currency then  d.volume
     else coalesce((select r.price * d.volume from rate r where  r.dt = d.dt and  r.currency_purchase = d.currency and  r.currency_sale = :currency), 0) 
     end 
    ) as volume_cur,
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
where  d.account_id = :account_id
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
group by d.ticker
    """)
    fun findAssets(@Param("account_id") accountId: Int,
                   @Param("currency") currency: Currency): List<AssetView>
}