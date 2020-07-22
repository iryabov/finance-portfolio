package com.github.iryabov.invest.etl

import com.github.iryabov.invest.entity.Asset
import com.github.iryabov.invest.entity.AssetHistory
import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.repository.AssetHistoryRepository
import com.github.iryabov.invest.repository.AssetRepository
import com.github.iryabov.invest.repository.SecuritiesClient
import com.github.iryabov.invest.repository.Security
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class AssetHistoryLoader(
        val securitiesClient: SecuritiesClient,
        val assetRepo: AssetRepository,
        val assetHistoryRepo: AssetHistoryRepository
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
                assetHistoryRepo.save(historyPrice.toHistoryEntity())
            }
            begin = end
        } while (end < till)
    }
}

private fun Security.toHistoryEntity(): AssetHistory {
    return AssetHistory(
            date = this.date,
            price = this.price,
            ticker = this.ticker)
}

private fun Security.toEntity(exists: Boolean): Asset {
    val asset = Asset(
            ticker = this.ticker,
            name = this.shortName,
            priceNow = this.price,
            currency = Currency.RUB)
    asset.newEntity = !exists
    return asset
}
