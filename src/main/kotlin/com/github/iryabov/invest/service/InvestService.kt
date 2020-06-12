package com.github.iryabov.invest.service

import com.github.iryabov.invest.model.AccountForm
import com.github.iryabov.invest.model.DialForm

interface InvestService {
    fun createAccount(form: AccountForm): Int

    fun deleteAccount(id: Int)

    fun addDial(accountId: Int, form: DialForm): Long

    fun deleteDial(accountId: Int, id: Long)

    fun deactivateDial(accountId: Int, id: Long)

}