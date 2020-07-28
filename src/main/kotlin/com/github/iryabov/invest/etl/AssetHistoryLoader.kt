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
import java.time.LocalDate

@Component
class AssetHistoryLoader(
        val securitiesClient: SecuritiesClient,
        val assetRepo: AssetRepository,
        val securityHistoryRepo: SecurityHistoryRepository
) {
    fun load(ticker: String, from: LocalDate, till: LocalDate) {
        val security = securitiesClient.findLastPrice(ticker)
        assetRepo.save(security.toEntity(assetRepo.existsById(ticker)))
        var begin = from
        var end = from
        do {
            end = if (till > begin.plusMonths(3)) begin.plusMonths(3) else till
            val historyPrices = securitiesClient.findHistoryPrices(ticker, begin, end)
            for (historyPrice in historyPrices) {
                val found = securityHistoryRepo.findByTickerAndDate(ticker, historyPrice.date)
                securityHistoryRepo.save(historyPrice.toHistoryEntity(found))
            }
            begin = end
        } while (end < till)
    }
}

private fun Security.toHistoryEntity(dest: SecurityHistory? = null): SecurityHistory {
    return SecurityHistory(
            date = this.date,
            price = this.price,
            ticker = this.ticker)
            .copy(id = dest?.id)
}

private fun Security.toEntity(exists: Boolean): Asset {
    val asset = Asset(
            ticker = this.ticker,
            name = this.shortName,
            priceNow = this.price,
            assetClass = AssetClass.SHARE,
            currency = Currency.RUB)
    asset.newEntity = !exists
    return asset
}
