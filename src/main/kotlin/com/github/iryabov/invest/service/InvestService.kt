package com.github.iryabov.invest.service

import com.github.iryabov.invest.entity.Dial
import com.github.iryabov.invest.model.*
import java.net.URI

interface InvestService {
    fun createAccount(form: AccountForm): Int

    fun deleteAccount(id: Int)

    fun addDial(accountId: Int, form: DialForm): Long

    fun deleteDial(accountId: Int, id: Long)

    fun deactivateDial(accountId: Int, id: Long)

    fun getAccount(accountId: Int): AccountView

    fun getAccounts(): List<AccountView>

    fun getAsset(accountId: Int, ticker: String): AssetView

    fun getDials(accountId: Int, ticker: String): List<DialView>
}