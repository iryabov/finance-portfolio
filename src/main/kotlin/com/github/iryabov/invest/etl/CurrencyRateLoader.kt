package com.github.iryabov.invest.etl

import com.github.iryabov.invest.client.CurrenciesClient
import com.github.iryabov.invest.client.ExchangeRate
import com.github.iryabov.invest.entity.CurrencyPair
import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.repository.CurrencyRateRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.stream.Collectors

@Component
class CurrencyRateLoader(
        val rateRepo: CurrencyRateRepository,
        @Qualifier("currenciesClientCBRF")
        val currenciesClient: CurrenciesClient
) {
    fun load(from: LocalDate, till: LocalDate) {
        val dates = from.datesUntil(till).collect(Collectors.toList())
        for (date in dates) {
            val exchange: ExchangeRate by lazy { currenciesClient.findCurrencyByDate(date) }
            for (currencyPurchased in Currency.values()) {
                val rates = rateRepo.findByDateAndBase(date, currencyPurchased)
                for (currencySale in Currency.values().filter { it != currencyPurchased }) {
                    if (rates.all { it.currencySale != currencySale }) {
                        rateRepo.save(CurrencyPair(
                                date = date,
                                currencyPurchased = currencyPurchased,
                                currencySale = currencySale,
                                price = exchange.getPairExchangePrice(currencyPurchased, currencySale)))
                    }
                }
            }
        }
    }
}