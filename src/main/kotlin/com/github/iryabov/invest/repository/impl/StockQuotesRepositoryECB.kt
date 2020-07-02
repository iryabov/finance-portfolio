package com.github.iryabov.invest.repository.impl

import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.repository.ExchangeRate
import com.github.iryabov.invest.repository.StockQuotesRepository
import org.springframework.stereotype.Repository
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Repository
class StockQuotesRepositoryECB : StockQuotesRepository {
    private val client: WebClient = WebClient.create("https://api.exchangeratesapi.io")

    override fun findCurrencyByDate(date: LocalDate): ExchangeRate {
        val dateStr = date.format(DateTimeFormatter.ISO_DATE)
        val base = Currency.USD
        val currencies = Currency.values().filter { c -> c != base }.joinToString(",")
        val response = client.get().uri("/${dateStr}?base=${base.name}&symbols=${currencies}")
                .retrieve().bodyToMono(Map::class.java).block()
        val ratesMap = response!!["rates"] as Map<*, *>
        val rates = ratesMap.mapKeys { e -> Currency.valueOf(e.key as String) }
                .mapValues { e -> BigDecimal(e.value as Double)  }
        return ExchangeRate(date = date, base = base, rates = rates)
    }

}