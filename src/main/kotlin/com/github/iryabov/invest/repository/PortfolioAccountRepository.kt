package com.github.iryabov.invest.repository

import com.github.iryabov.invest.entity.PortfolioAccount
import com.github.iryabov.invest.entity.PortfolioAccountID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.CrudRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class PortfolioAccountRepository : CrudRepository<PortfolioAccount, PortfolioAccountID> {
    @Autowired
    private lateinit var jdbc: JdbcTemplate

    override fun <S : PortfolioAccount?> save(entity: S): S {
        jdbc.update("insert into portfolio_account (portfolio_id, account_id) values (?, ?)",
                entity!!.id.portfolioId, entity.id.accountId)
        return entity
    }

    override fun <S : PortfolioAccount?> saveAll(entities: MutableIterable<S>): MutableIterable<S> {
        entities.forEach { save(it) }
        return entities
    }

    fun findAllByPortfolioId(portfolioId: Int): List<PortfolioAccount> {
        return jdbc.query("""
            select 
                 pa.account_id as account_id,
                 a.name as account_name
            from portfolio_account pa
            join account a on a.id = pa.account_id
            where pa.portfolio_id = ?
        """.trimMargin(), arrayOf(portfolioId)) { rs, _ ->
            PortfolioAccount(
                    PortfolioAccountID(portfolioId, rs.getInt("account_id")),
                    rs.getString("account_name"))
        }
    }

    override fun findById(id: PortfolioAccountID): Optional<PortfolioAccount> {
        TODO("Not yet implemented")
    }

    override fun existsById(id: PortfolioAccountID): Boolean {
        TODO("Not yet implemented")
    }

    override fun findAll(): MutableIterable<PortfolioAccount> {
        TODO("Not yet implemented")
    }

    override fun findAllById(ids: MutableIterable<PortfolioAccountID>): MutableIterable<PortfolioAccount> {
        TODO("Not yet implemented")
    }

    override fun count(): Long {
        TODO("Not yet implemented")
    }

    override fun deleteById(id: PortfolioAccountID) {
        TODO("Not yet implemented")
    }

    override fun delete(entity: PortfolioAccount) {
        jdbc.update("delete from portfolio_account where portfolio_id = ? and account_id = ?",
                entity.id.portfolioId, entity.id.accountId)
    }

    override fun deleteAll(entities: MutableIterable<PortfolioAccount>) {
        entities.forEach { delete(it) }
    }

    override fun deleteAll() {
        TODO("Not yet implemented")
    }


}