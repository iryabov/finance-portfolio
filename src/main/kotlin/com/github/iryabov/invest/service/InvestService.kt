package com.github.iryabov.invest.service

import com.github.iryabov.invest.entity.Asset
import com.github.iryabov.invest.model.*
import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.relation.Period

interface InvestService {
    fun createAccount(form: AccountForm): Int

    fun deleteAccount(id: Int)

    fun addDeal(accountId: Int, form: DealForm): Long

    fun remittanceDeal(accountFrom: Int, accountTo: Int, form: RemittanceForm): Pair<Long, Long>

    fun deleteDeal(accountId: Int, id: Long)

    fun deactivateDeal(accountId: Int, id: Long)

    fun getAccount(accountId: Int, currency: Currency = Currency.RUB): AccountView

    fun getAccounts(): List<AccountView>

    fun getAsset(accountId: Int, currency: Currency = Currency.RUB, ticker: String): AssetView

    fun getAssetHistory(accountId: Int, ticker: String, period: Period, currency: Currency = Currency.RUB): List<AssetHistoryView>

    fun getDeals(accountId: Int, currency: Currency = Currency.RUB, ticker: String? = null): List<DealView>

    fun getSecurities(): List<SecurityView>

    fun addSecurity(form: Asset)

    fun editSecurity(form: Asset)

    fun deleteSecurity(ticker: String)

    fun getSecurity(ticker: String): SecurityView

    fun getSecurity(ticker: String,
                    period: Period = Period.FIVE_YEARS,
                    currency: Currency = Currency.RUB): SecurityView

    fun getCurrency(pair1: Currency, pair2: Currency,
                    period: Period): CurrencyView
}