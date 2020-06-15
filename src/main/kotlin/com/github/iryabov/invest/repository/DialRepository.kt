package com.github.iryabov.invest.repository

import com.github.iryabov.invest.entity.Dial
import com.github.iryabov.invest.model.AccountAssetView
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.math.BigInteger


@Repository
interface DialRepository : CrudRepository<Dial, Long> {
    @Modifying
    @Query("UPDATE dial SET active = NOT active WHERE id = :id")
    fun deactivate(@Param("id") id: Long): Boolean

    @Query("""
            SELECT sum(d.amount) as total_deposit
            FROM dial d 
            WHERE d.account_id = :account_id
              AND d.type = 'DEPOSIT'
              AND d.active = true
            """)
    fun findTotalDeposit(@Param("account_id") accountId: Int): BigDecimal?

    @Query("""
            SELECT sum(d.amount) as total_withdrawals
            FROM dial d 
            WHERE d.account_id = :account_id
              AND d.type = 'WITHDRAWALS'
              AND d.active = true
            """)
    fun findTotalWithdrawals(@Param("account_id") accountId: Int): BigDecimal?

    @Query("""
        SELECT d.ticker as asset_ticker, 
               a.name as asset_name,
               a.class as asset_class,
               a.price_now as current_price,
               d.amount as amount,
               d.quantity as quantity,
               d.spent as spent,
               d.received as received
        FROM (
            SELECT d.ticker as ticker,
                   sum(CASE 
                       WHEN d.quantity > 0 THEN (d.quantity - d._sold_quantity) * (d.amount/d.quantity)
                       ELSE 0 
                       END
                   ) as amount, 
                   sum(d.quantity) as quantity,
                   sum(CASE 
                       WHEN d.amount < 0 THEN -1 * d.amount
                       ELSE 0 
                       END 
                   ) as spent,
                   sum(CASE 
                       WHEN d.amount > 0 THEN d.amount
                       ELSE 0 
                       END) as received
            FROM dial d
            WHERE d.account_id = :account_id
              AND d.active = true
            GROUP BY d.ticker
        ) as d
        LEFT JOIN asset a ON a.ticker = d.ticker        
        ORDER BY d.ticker 
    """)
    fun findAssets(@Param("account_id") accountId: Int): List<AccountAssetView>
}