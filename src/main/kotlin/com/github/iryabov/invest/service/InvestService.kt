package com.github.iryabov.invest.service

import com.github.iryabov.invest.entity.Asset
import com.github.iryabov.invest.entity.Remittance
import com.github.iryabov.invest.model.*
import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.relation.Period
import com.github.iryabov.invest.relation.TargetType
import java.math.BigDecimal
import java.time.LocalDate

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

    fun getRemittanceDials(): List<RemittanceView>

    fun getSecurities(): List<SecurityView>

    fun getSecurities(name: String): List<SecurityView>

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

    fun getPortfolioSummary(portfolioId: Int,
                            year: Int, month: Int? = null,
                            currency: Currency = Currency.RUB): PortfolioSummaryView

    fun getPortfolioHistory(portfolioId: Int,
                            period: Period = Period.FIVE_YEARS,
                            currency: Currency = Currency.RUB): List<TargetHistoryView>

    fun getPortfolioBenchmark(portfolioId: Int,
                            period: Period = Period.FIVE_YEARS,
                            currency: Currency = Currency.RUB,
                            benchmark: String): List<TargetBenchmarkView>

    fun createPortfolio(form: PortfolioForm): Int

    fun deletePortfolio(id: Int)

    fun getPortfolioForm(id: Int): PortfolioForm

    fun updatePortfolio(id: Int, form: PortfolioForm)

    fun addAsset(portfolioId: Int, ticker: String): Int

    fun addAssets(portfolioId: Int, tickers: List<String>)

    fun addAssets(portfolioId: Int, criteria: SecurityCriteria)

    //todo (portfolioId: Int, type: TargetType, currency: Currency = Currency.RUB)
    fun getTargets(currency: Currency = Currency.RUB, portfolioId: Int, type: TargetType): List<TargetView>

    //todo (portfolioId: Int, ticker: String, currency: Currency = Currency.RUB)
    fun getTarget(currency: Currency = Currency.RUB, portfolioId: Int, ticker: String): AssetView

    fun saveTarget(portfolioId: Int, type: TargetType, ticker: String, proportion: Int): Int

    fun saveTargets(portfolioId: Int, type: TargetType, data: Map<String, Int>): Map<String, Int>

    fun deactivateTarget(portfolioId: Int, ticker: String)

    fun deleteTarget(portfolioId: Int, ticker: String)

    fun updateTarget(portfolioId: Int, ticker: String, form: TargetForm)

    fun getTargetCandidates(portfolioId: Int, criteria: SecurityCriteria): List<SecurityView>

    fun getAnalytics(type: TargetType, portfolioId: Int, currency: Currency = Currency.RUB): List<ChartView>

    fun getTargetProportions(portfolioId: Int, type: TargetType): List<ChartView>

    fun getBalancedAssets(portfolioId: Int, currency: Currency = Currency.RUB): BalancedView

    fun resetAssetTargets(portfolioId: Int)

    fun plusAssetTarget(portfolioId: Int, ticker: String, amount: BigDecimal)

    fun minusAssetTarget(portfolioId: Int, ticker: String, amount: BigDecimal)
}
