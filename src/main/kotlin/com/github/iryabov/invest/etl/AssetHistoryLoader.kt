package com.github.iryabov.invest.etl

import com.github.iryabov.invest.entity.AssetHistory
import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.repository.AssetHistoryRepository
import com.github.iryabov.invest.repository.SecuritiesClient
import com.github.iryabov.invest.repository.Security
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class AssetHistoryLoader(
        @Autowired
        val securitiesClient: SecuritiesClient,
        @Autowired
        val assetHistoryRepo: AssetHistoryRepository
) {
    fun load(ticker: String, from: LocalDate, till: LocalDate) {
        var begin = from
        var end = from
        do {
            end = if (till > begin.plusMonths(3)) begin.plusMonths(3) else till
            val historyPrices = securitiesClient.findHistoryPrices(ticker, begin, end)
            for (historyPrice in historyPrices) {
                assetHistoryRepo.save(historyPrice.toEntity())
            }
            begin = end
        } while (end < till)
    }
}

private fun Security.toEntity(): AssetHistory {
    return AssetHistory(
            date = this.date,
            price = this.price,
            ticker = this.ticker,
            currency = Currency.RUB)
}
