package com.github.iryabov.invest.etl

import com.github.iryabov.invest.entity.Asset
import com.github.iryabov.invest.entity.SecurityHistory
import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.repository.SecurityHistoryRepository
import com.github.iryabov.invest.repository.AssetRepository
import com.github.iryabov.invest.client.SecuritiesClient
import com.github.iryabov.invest.client.Security
import com.github.iryabov.invest.client.impl.SecuritiesClientCache
import com.github.iryabov.invest.client.impl.SecuritiesClientMoex
import com.github.iryabov.invest.client.impl.SecuritiesClientUnibit
import com.github.iryabov.invest.relation.AssetClass
import com.github.iryabov.invest.relation.FinanceApi
import com.github.iryabov.invest.service.impl.P1
import com.github.iryabov.invest.service.impl.isCurrency
import org.springframework.stereotype.Component
import java.math.RoundingMode
import java.time.LocalDate
import java.time.Period
import java.util.*
import java.util.stream.Collectors

@Component
class AssetHistoryLoader(
        val securitiesClientMoex: SecuritiesClientMoex,
        val securitiesClientUnibit: SecuritiesClientUnibit,
        val securitiesClientCache: SecuritiesClientCache,
        val assetRepo: AssetRepository,
        val securityHistoryRepo: SecurityHistoryRepository,
        val currencyRateLoader: CurrencyRateLoader
) {
    fun load(ticker: String, from: LocalDate, till: LocalDate) {
        var client: SecuritiesClient = securitiesClientMoex
        val assetFound = assetRepo.findById(ticker).orElseThrow()
        client = when (assetFound.api) {
            FinanceApi.MOEX -> securitiesClientMoex
            FinanceApi.UNIBIT -> securitiesClientUnibit
            FinanceApi.CACHE -> securitiesClientCache
        }
        val security = client.findLastPrice(ticker)
        exchange(security, assetFound.currency ?: Currency.RUB)
        val asset = assetRepo.save(security.toEntity(assetFound))
        var begin = from
        var end = from
        do {
            end = if (till > begin.plusMonths(3)) begin.plusMonths(3) else till
            val historyPrices = client.findHistoryPrices(ticker, begin, end)
            for (historyPrice in historyPrices) {
                val found = securityHistoryRepo.findByTickerAndDate(ticker, historyPrice.date)
                exchange(historyPrice, asset.currency ?: Currency.RUB)
                securityHistoryRepo.save(historyPrice.toHistoryEntity(found))
            }
            begin = end
        } while (end < till)
    }

    private fun exchange(historyPrice: Security, faceCurrency: Currency) {
        if (historyPrice.settlementCurrency != faceCurrency) {
            val rate = currencyRateLoader.getRate(historyPrice.date, faceCurrency, historyPrice.settlementCurrency)
            historyPrice.facePrice = historyPrice.settlementPrice.divide(rate, 2, RoundingMode.HALF_UP)
        } else {
            historyPrice.facePrice = historyPrice.settlementPrice
        }
    }
}

private fun Security.toHistoryEntity(dest: SecurityHistory? = null): SecurityHistory {
    return SecurityHistory(
            date = this.date,
            price = this.facePrice,
            ticker = this.ticker)
            .copy(id = dest?.id)
}

private fun Security.toEntity(exists: Asset): Asset {
    return exists.copy(
                ticker = this.ticker,
                name = this.shortName,
                priceNow = this.facePrice)
}
