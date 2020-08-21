package com.github.iryabov.invest.etl

import com.github.iryabov.invest.entity.Asset
import com.github.iryabov.invest.entity.SecurityHistory
import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.repository.SecurityHistoryRepository
import com.github.iryabov.invest.repository.AssetRepository
import com.github.iryabov.invest.client.SecuritiesClient
import com.github.iryabov.invest.client.Security
import com.github.iryabov.invest.relation.AssetClass
import org.springframework.stereotype.Component
import java.math.RoundingMode
import java.time.LocalDate
import java.util.*

@Component
class AssetHistoryLoader(
        val securitiesClient: SecuritiesClient,
        val assetRepo: AssetRepository,
        val securityHistoryRepo: SecurityHistoryRepository,
        val currencyRateLoader: CurrencyRateLoader
) {
    fun load(ticker: String, from: LocalDate, till: LocalDate) {
        val security = securitiesClient.findLastPrice(ticker)
        val assetOptional = assetRepo.findById(ticker)
        if (assetOptional.isPresent)
            exchange(security, assetOptional.get())
        val asset = assetRepo.save(security.toEntity(assetOptional))
        var begin = from
        var end = from
        do {
            end = if (till > begin.plusMonths(3)) begin.plusMonths(3) else till
            val historyPrices = securitiesClient.findHistoryPrices(ticker, begin, end)
            for (historyPrice in historyPrices) {
                val found = securityHistoryRepo.findByTickerAndDate(ticker, historyPrice.date)
                exchange(historyPrice, asset)
                securityHistoryRepo.save(historyPrice.toHistoryEntity(found))
            }
            begin = end
        } while (end < till)
    }

    private fun exchange(historyPrice: Security, asset: Asset) {
        if (historyPrice.settlementCurrency != asset.currency) {
            val rate = currencyRateLoader.getRate(historyPrice.date, asset.currency ?: Currency.RUB, historyPrice.settlementCurrency)
            historyPrice.price = historyPrice.price.divide(rate, 2, RoundingMode.HALF_UP)
        }
    }
}

private fun Security.toHistoryEntity(dest: SecurityHistory? = null): SecurityHistory {
    return SecurityHistory(
            date = this.date,
            price = this.price,
            ticker = this.ticker)
            .copy(id = dest?.id)
}

private fun Security.toEntity(exists: Optional<Asset>): Asset {
    return if (exists.isPresent) {
        exists.get().copy(
                ticker = this.ticker,
                name = this.shortName,
                priceNow = this.price)
    } else {
        val asset = Asset(
                ticker = this.ticker,
                name = this.shortName,
                priceNow = this.price,
                assetClass = AssetClass.SHARE,
                currency = this.settlementCurrency)
        asset.newEntity = true
        asset
    }
}
