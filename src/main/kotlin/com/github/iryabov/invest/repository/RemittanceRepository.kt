package com.github.iryabov.invest.repository

import com.github.iryabov.invest.entity.Remittance
import com.github.iryabov.invest.model.RemittanceView
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface RemittanceRepository: CrudRepository<Remittance, Long> {
    @Query("""
        select *
        from remittance
        where dial_from = :dial_id or dial_to = :dial_id
    """)
    fun findOneByDialFromOrTo(@Param("dial_id") dialId: Long): Remittance?

    @Query("""
select 
    d.dt as opened,
    (case when d.type = 'WITHDRAWALS' then a.name else null end) as account_from, 
    (case when at.id is not null then at.name when d.type = 'DEPOSIT' then a.name else null end) as account_to,
    d.currency as currency,
    abs(d.quantity) as quantity
from dial d
join account a on d.account_id = a.id 
left join remittance r on r.dial_from = d.id
left join dial dt on r.dial_to = dt.id
left join account at on dt.account_id = at.id
where d.type in ('WITHDRAWALS', 'DEPOSIT')
  and not exists (select id from remittance rf where rf.dial_to = d.id)
order by d.dt desc
    """)
    fun findAllOrderByOpenedDesc(): List<RemittanceView>
}