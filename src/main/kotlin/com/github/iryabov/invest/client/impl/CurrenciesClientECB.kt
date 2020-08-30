package com.github.iryabov.invest.client.impl

import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.client.ExchangeRate
import com.github.iryabov.invest.client.CurrenciesClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Repository
class CurrenciesClientECB : CurrenciesClient {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val client: WebClient = WebClient.create("https://api.exchangeratesapi.io")

    override fun findCurrencyByDate(date: LocalDate): ExchangeRate {
        val dateStr = date.format(DateTimeFormatter.ISO_DATE)
        val base = Currency.USD
        val currencies = Currency.values().filter { c -> c != base }.joinToString(",")
        val url = "/${dateStr}?base=${base.name}&symbols=${currencies}"
        logger.info("ecb $url")
        val response = client.get().uri(url)
                .retrieve().bodyToMono(Map::class.java).block()
        val ratesMap = response!!["rates"] as Map<*, *>
        val rates = ratesMap.mapKeys { e -> Currency.valueOf(e.key as String) }
                .mapValues { e -> BigDecimal(e.value as Double)  }
        return ExchangeRate(date = date, base = base, rates = rates)
    }

}