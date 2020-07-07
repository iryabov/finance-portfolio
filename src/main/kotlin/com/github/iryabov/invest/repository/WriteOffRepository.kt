package com.github.iryabov.invest.repository

import com.github.iryabov.invest.entity.WriteOff
import com.github.iryabov.invest.model.Balance
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface WriteOffRepository: CrudRepository<WriteOff, Long> {
    @Query("""
select 
    d.dial_from,
    d.purchased_quantity,
    d.sold_quantity
from (
select 
    d.id as dial_from,
    d.dt as dt,
    d.quantity as purchased_quantity,
    (
        SELECT sum(w.quantity) as sold_quantity
        FROM writeoff w
        JOIN dial d2 ON d2.id = w.dial_to
        WHERE w.dial_from = d.id
          AND d2.dt <= :date_from
    ) as sold_quantity    
from dial d
where d.active = true
  and d.quantity > 0
  and d.ticker = :ticker         
  and d.account_id = :account_id
  and d.dt <= :date_from
union 
select 
    d.id as dial_from,
    d.dt as dt,
    d.volume as purchased_quantity,
    (
    SELECT sum(w.quantity) as sold_quantity
    FROM writeoff w
    JOIN dial d2 ON d2.id = w.dial_to
    WHERE w.dial_from = d.id
      AND d2.dt <= :date_from
    ) as sold_quantity   
from dial d
where d.active = true
  and d.volume > 0
  and d.ticker != d.currency
  and d.account_id = :account_id
  and d.currency = :ticker   
  and d.dt <= :date_from
) d
order by d.dt, d.dial_from
    """)
    fun findBalance(@Param("account_id") accountId: Int,
                    @Param("ticker") ticker: String,
                    @Param("date_from") dateFrom: LocalDate): List<Balance>
}