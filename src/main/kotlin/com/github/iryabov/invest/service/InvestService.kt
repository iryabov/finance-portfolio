package com.github.iryabov.invest.service

import com.github.iryabov.invest.entity.SecurityHistory
import com.github.iryabov.invest.model.*
import com.github.iryabov.invest.relation.Period

interface InvestService {
    fun createAccount(form: AccountForm): Int

    fun deleteAccount(id: Int)

    fun addDial(accountId: Int, form: DialForm): Long

    fun deleteDial(accountId: Int, id: Long)

    fun deactivateDial(accountId: Int, id: Long)

    fun getAccount(accountId: Int): AccountView

    fun getAccounts(): List<AccountView>

    fun getAsset(accountId: Int, ticker: String): AssetView

    fun getAssetHistory(accountId: Int, ticker: String, period: Period): List<AssetHistoryView>

    fun getDials(accountId: Int, ticker: String): List<DialView>

    fun getSecurity(ticker: String,
                    period: Period): SecurityView
}