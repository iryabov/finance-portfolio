package com.github.iryabov.invest.client.impl

import com.github.iryabov.invest.client.SecuritiesClient
import com.github.iryabov.invest.client.Security
import com.github.iryabov.invest.relation.Currency
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Repository
import org.springframework.web.reactive.function.client.WebClient
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * UnitBit Client
 * Example:
 * https://api.unibit.ai/v2/stock/historical?tickers=AAPL&interval=1&startDate=2019-01-01&endDate=2019-01-31&selectedFields=all&accessKey=Demo
 * {@sample
 * {
 *   "meta_data" : {
 *       "api_name" : "historical_stock_price_v2",
 *       "num_total_data_points" : 1,
 *       "credit_cost" : 10,
 *       "start_date" : "2019-01-01",
 *       "end_date" : "2019-01-2"
 *   },
 *   "result_data" : {
 *       "AAPL" : [ {
 *           "date" : "2019-01-02",
 *           "volume" : 37039700,
 *           "high" : 158.85,
 *           "low" : 154.23,
 *           "adj_close" : 156.642,
 *           "close" : 157.92,
 *           "open" : 154.89
 *       } ]
 *    }
 *   }
 * }
 *
 */
@Repository
class SecuritiesClientUnibit: SecuritiesClient {
    private val client: WebClient = WebClient.create("https://api.unibit.ai/v2")
    @Value("\${portfolio.client.unibit.access_key}")
    private lateinit var accessKey: String

    override fun findHistoryPrices(name: String, from: LocalDate, till: LocalDate): List<Security> {
        val fromStr = from.format(DateTimeFormatter.ISO_DATE)
        val tillStr = till.format(DateTimeFormatter.ISO_DATE)
        return retrieve(name, "/stock/historical?tickers=$name&interval=1&startDate=$fromStr&endDate=$tillStr&selectedFields=all&accessKey=$accessKey")
    }

    override fun findLastPrice(name: String): Security {
        val history = retrieve(name, "/stock/historical?tickers=$name&selectedFields=all&accessKey=$accessKey")
        if (history.isEmpty())
            throw IllegalStateException("Ticker $name not found")
        return history[0]
    }

    override fun findByName(name: String): List<Security> {
        return Collections.emptyList()
    }

    private fun retrieve(name: String, url: String): List<Security> {
        val response = client.get().uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .acceptCharset(Charsets.UTF_8)
                .retrieve().bodyToMono(Map::class.java).block()
                ?: return Collections.emptyList()
        if (response["Information"] != null)
            throw IllegalStateException(response["Information"] as String)
        val data = response["result_data"] as Map<*, *>
        if (data.isEmpty()) return Collections.emptyList()
        val history = data[name] as List<*>
        val list = ArrayList<Security>()
        for (item in history.reversed()) {
            val day = item as Map<*, *>
            list.add(Security(
                    date = LocalDate.parse(day["date"] as String, DateTimeFormatter.ISO_DATE),
                    ticker = name,
                    settlementPrice = BigDecimal(day["close"] as Double),
                    settlementCurrency = Currency.USD)
            )
        }
        return list
    }
}