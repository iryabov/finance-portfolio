package com.github.iryabov.invest.client.impl

import com.github.iryabov.invest.client.SecuritiesClient
import com.github.iryabov.invest.client.Security
import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.repository.AssetRepository
import com.github.iryabov.invest.service.impl.P1
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.Period
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList

@Repository
class SecuritiesClientCache(
        val assetRepository: AssetRepository
): SecuritiesClient {
    override fun findHistoryPrices(name: String, from: LocalDate, till: LocalDate): List<Security> {
        val asset = assetRepository.findByIdOrNull(name)
        val result = ArrayList<Security>()
        val datesUntil = from.datesUntil(till, Period.ofDays(1)).collect(Collectors.toList())
        datesUntil.stream().forEach {
            result.add(Security(
                    date = it,
                    ticker = name,
                    settlementCurrency = asset?.currency ?: Currency.RUB,
                    settlementPrice = P1))
        }
        return result
    }

    override fun findLastPrice(name: String): Security {
        val asset = assetRepository.findByIdOrNull(name)
        val lastDate = LocalDate.now()
        return Security(
                date = lastDate,
                ticker = name,
                settlementCurrency = asset?.currency ?: Currency.RUB,
                settlementPrice = P1)
    }

    override fun findByName(name: String): List<Security> {
        return Collections.emptyList()
    }
}