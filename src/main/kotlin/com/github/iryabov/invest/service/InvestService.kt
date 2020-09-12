package com.github.iryabov.invest.service

import com.github.iryabov.invest.entity.Asset
import com.github.iryabov.invest.model.*
import com.github.iryabov.invest.relation.AnalyticsType
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

    fun getAccounts(currency: Currency = Currency.RUB): TotalView

    fun getAsset(accountId: Int, currency: Currency = Currency.RUB, ticker: String): AssetView

    fun getAssetHistory(accountId: Int? = null, ticker: String, period: Period, currency: Currency = Currency.RUB): List<AssetHistoryView>

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

    fun getPortfolios(currency: Currency = Currency.RUB): List<PortfolioView>

    //todo (portfolioId: Int, currency: Currency = Currency.RUB)
    fun getPortfolio(currency: Currency = Currency.RUB, portfolioId: Int): PortfolioView

    fun createPortfolio(form: PortfolioForm): Int

    fun deletePortfolio(id: Int)

    fun addTarget(portfolioId: Int, ticker: String)

    fun addTargets(portfolioId: Int, criteria: SecurityCriteria)

    fun deactivateTarget(portfolioId: Int, ticker: String)

    fun deleteTarget(portfolioId: Int, ticker: String)

    fun updateTarget(portfolioId: Int, ticker: String, form: TargetForm)

    //todo (portfolioId: Int, ticker: String, currency: Currency = Currency.RUB)
    fun getTarget(currency: Currency = Currency.RUB, portfolioId: Int, ticker: String): AssetView

    fun getTargetCandidates(portfolioId: Int, criteria: SecurityCriteria): List<SecurityView>

    fun getAnalytics(type: AnalyticsType, portfolioId: Int, currency: Currency = Currency.RUB): List<ChartView>
}