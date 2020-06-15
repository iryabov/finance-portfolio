package com.github.iryabov.invest.service.impl

import com.github.iryabov.invest.entity.Account
import com.github.iryabov.invest.entity.Dial
import com.github.iryabov.invest.model.AccountAssetView
import com.github.iryabov.invest.model.AccountForm
import com.github.iryabov.invest.model.AccountView
import com.github.iryabov.invest.model.DialForm
import com.github.iryabov.invest.repository.AccountRepository
import com.github.iryabov.invest.repository.DialRepository
import com.github.iryabov.invest.service.InvestService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.math.abs

@Service
@Transactional
class InvestServiceImpl(
        val accountRepo: AccountRepository,
        val dialRepo: DialRepository
) : InvestService {
    override fun createAccount(form: AccountForm): Int {
        val created = accountRepo.save(form.toEntity())
        return created.id!!
    }

    override fun deleteAccount(id: Int) {
        accountRepo.deleteById(id)
    }

    override fun addDial(accountId: Int, form: DialForm): Long {
        val created = dialRepo.save(form.toEntityWith(accountId))
        return created.id!!
    }

    override fun deleteDial(accountId: Int, id: Long) {
        dialRepo.deleteById(id)
    }

    override fun deactivateDial(accountId: Int, id: Long) {
        dialRepo.deactivate(id)
    }

    override fun getAccount(accountId: Int): AccountView {
        val totalDeposit = (dialRepo.findTotalDeposit(accountId) ?: BigDecimal.ZERO).abs()
        val totalWithdrawals = (dialRepo.findTotalWithdrawals(accountId) ?: BigDecimal.ZERO).abs()
        val assets = ArrayList<AccountAssetView>()
        assets.addAll(dialRepo.findAssets(accountId))
        return AccountView(
                totalDeposit = totalDeposit,
                totalWithdrawals = totalWithdrawals ?: BigDecimal.ZERO,
                assets = assets)
    }
}

private fun DialForm.toEntityWith(accountId: Int) = Dial(
        accountId = accountId,
        type = type,
        ticker = ticker,
        dateOpen = opened ?: LocalDate.now(),
        currency = currency,
        amount = amount,
        quantity = quantity
)

fun AccountForm.toEntity() = Account(
        name = name,
        num = num
)
