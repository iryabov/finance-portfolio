package com.github.iryabov.invest.client.impl

import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.client.ExchangeRate
import com.github.iryabov.invest.client.CurrenciesClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import org.springframework.web.reactive.function.client.WebClient
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Repository
class CurrenciesClientECB : CurrenciesClient {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val client: WebClient = WebClient.create("http://api.exchangeratesapi.io")
    @Value("\${portfolio.client.apilayer.access_key}")
    private val key: String? = null

    override fun findCurrencyByDate(date: LocalDate): ExchangeRate {
        val dateStr = date.format(DateTimeFormatter.ISO_DATE)
        val base = Currency.EUR
        val currencies = Currency.values().filter { c -> c != base }.joinToString(",")
        val url = "/v1/${dateStr}?symbols=${currencies}&access_key=${key}"
        logger.info("ecb $url")
        val response = client.get().uri(url)
            .retrieve().bodyToMono(Map::class.java).block() as Map<*, *>
        if (response["error"] != null)
            throw IllegalStateException((response["error"] as Map<*, *>)["info"] as String)
        val ratesMap = response["rates"] as Map<*, *>
        val rates = ratesMap.mapKeys { e -> Currency.valueOf(e.key as String) }
                .mapValues { e -> BigDecimal(e.value as Double)  }
        return ExchangeRate(date = date, base = base, rates = rates)
    }

}