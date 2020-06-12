package com.github.iryabov.invest.service.impl

import com.github.iryabov.invest.entity.Account
import com.github.iryabov.invest.entity.Dial
import com.github.iryabov.invest.model.AccountForm
import com.github.iryabov.invest.model.DialForm
import com.github.iryabov.invest.repository.AccountRepository
import com.github.iryabov.invest.repository.DialRepository
import com.github.iryabov.invest.service.InvestService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional
class InvestServiceImpl(
        val accountRepo: AccountRepository,
        val dialRepo: DialRepository
) : InvestService {
    override fun createAccount(form: AccountForm): Int {
        val created = accountRepo.save(form.toEntityWith())
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

fun AccountForm.toEntityWith() = Account(
        name = name,
        num = num
)
