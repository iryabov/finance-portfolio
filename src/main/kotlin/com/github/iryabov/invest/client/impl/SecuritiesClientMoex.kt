package com.github.iryabov.invest.client.impl

import com.github.iryabov.invest.client.Security
import com.github.iryabov.invest.client.SecuritiesClient
import com.github.iryabov.invest.relation.Currency
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Repository
import org.springframework.web.reactive.function.client.WebClient
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.StringReader
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.collections.ArrayList

@Repository
class SecuritiesClientMoex: SecuritiesClient {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val client: WebClient = WebClient.create("https://iss.moex.com/iss")

    override fun findHistoryPrices(name: String, from: LocalDate, till: LocalDate): List<Security> {
        val (market, board, ticker) = findMarketAndBoard(name)
        val fromStr = from.format(DateTimeFormatter.ISO_DATE)
        val tillStr = till.format(DateTimeFormatter.ISO_DATE)
        val response = callCandlebordes(market, board, ticker, fromStr, tillStr)
        val rows = readRows(response, "history")
        if (rows == null || rows.length == 0)
            return Collections.emptyList()
        val list =  ArrayList<Security>()
        for (i in 0 until rows.length) {
            val row = rows.item(i) as Element
            if (!row.getAttribute("CLOSE").isNullOrEmpty())
                list.add(Security(
                        date = LocalDate.parse(row.getAttribute("TRADEDATE"), DateTimeFormatter.ISO_DATE),
                        ticker = row.getAttribute("SECID"),
                        shortName = row.getAttribute("SHORTNAME"),
                        settlementPrice = getPrice(row.getAttribute("CLOSE"), market),
                        settlementCurrency = currencyOf(row.getAttribute("CURRENCYID")),
                        faceCurrency = currencyOf(row.getAttribute("FACEUNIT"))
                ))
        }
        return list
    }

    override fun findLastPrice(name: String): Security {
        val (market, board, ticker) = findMarketAndBoard(name)
        val response = callTicker(market, board, ticker)
        val securities = readRows(response)
        if (securities == null || securities.length == 0)
            throw IllegalStateException("Security $ticker not found")
        val sec0 = securities.item(0) as Element
        assert(sec0.getAttribute("SECID") == ticker)
        if (board == "SNDX") {
            val marketdata = readRows(response, "marketdata")
            if (marketdata == null || marketdata.length == 0)
                throw IllegalStateException("Marketdata $ticker not found")
            val data0 = marketdata.item(0) as Element
            assert(data0.getAttribute("SECID") == ticker)
            return Security(
                    date = LocalDate.parse(data0.getAttribute("TRADEDATE"), DateTimeFormatter.ISO_DATE),
                    ticker = data0.getAttribute("SECID"),
                    shortName = sec0.getAttribute("SHORTNAME"),
                    settlementPrice = BigDecimal(data0.getAttribute("CURRENTVALUE")),
                    settlementCurrency = currencyOf(sec0.getAttribute("CURRENCYID")),
                    faceCurrency = currencyOf(sec0.getAttribute("FACEUNIT")))
        } else {
            return Security(
                    date = LocalDate.parse(sec0.getAttribute("PREVDATE"), DateTimeFormatter.ISO_DATE),
                    ticker = sec0.getAttribute("SECID"),
                    shortName = sec0.getAttribute("SHORTNAME"),
                    settlementPrice = getPrice(sec0.getAttribute("PREVADMITTEDQUOTE"), market),
                    settlementCurrency = currencyOf(sec0.getAttribute("CURRENCYID")),
                    faceCurrency = currencyOf(sec0.getAttribute("FACEUNIT")))
        }
    }

    private fun getPrice(price: String, market: String): BigDecimal {
        val k = if (market == "bonds") 10 else 1
        return BigDecimal(k) * BigDecimal(price)
    }

    override fun findByName(name: String): List<Security> {
        val response = callSecurities(name)
        val rows = readRows(response)
        if (rows == null || rows.length == 0)
            return Collections.emptyList()
        val list =  ArrayList<Security>()
        for (i in 0 until rows.length) {
            val row = rows.item(i) as Element
            list.add(Security(
                    date = LocalDate.now(),
                    ticker = row.getAttribute("secid"),
                    shortName = row.getAttribute("shortname"),
                    fullName = row.getAttribute("name"))
            )
        }
        return list
    }

    private fun callCandlebordes(market: String, board: String, ticker: String, fromStr: String?, tillStr: String?): String? {
        val url = "/history/engines/stock/markets/$market/boards/$board/securities/$ticker/candlebordes.xml?from=$fromStr&till=$tillStr&iss.meta=off"
        logger.info("moex $url")
        val response = client.get().uri(url)
                .accept(MediaType.APPLICATION_XML)
                .acceptCharset(Charsets.UTF_8)
                .retrieve().bodyToMono(String::class.java).block()
        return response
    }

    private fun callTicker(market: String, board: String, ticker: String): String? {
        val url = "/engines/stock/markets/$market/boards/$board/securities/$ticker.xml?iss.meta=off"
        logger.info("moex $url")
        val response = client.get().uri(url)
                .accept(MediaType.APPLICATION_XML)
                .acceptCharset(Charsets.UTF_8)
                .retrieve().bodyToMono(String::class.java).block()
        return response
    }

    private fun callSecurities(name: String): String? {
        val url = "/securities.xml?q=$name&iss.meta=off"
        logger.info("moex $url")
        val response = client.get().uri(url)
                .accept(MediaType.APPLICATION_XML)
                .acceptCharset(Charsets.UTF_8)
                .retrieve().bodyToMono(String::class.java).block()
        return response
    }

    private fun readRows(response: String?, type: String = "securities"): NodeList? {
        val xml = readXml(response!!)
        val document = xml.getElementsByTagName("document").item(0) as Element
        val securities = findData(document, type)
        return (securities.getElementsByTagName("rows").item(0) as Element)
                .getElementsByTagName("row")
    }

    private fun findMarketAndBoard(name: String): Triple<String, String, String> {
        val tickerResponse = callSecurities(name)
        val tickerRows = readRows(tickerResponse)
        var tickerRow: Element? = null
        for (i in 0 until tickerRows!!.length) {
            val row = tickerRows.item(i) as Element
            if (row.getAttribute("secid") == name) {
                tickerRow = row
                break
            }
        }
        if (tickerRow == null)
            throw IllegalStateException("Ticker $name not found")
        val market = when {
            tickerRow.getAttribute("type").contains("share") -> "shares"
            tickerRow.getAttribute("type").contains("etf") -> "shares"
            tickerRow.getAttribute("type").contains("depositary") -> "shares"
            tickerRow.getAttribute("type").contains("bond") -> "bonds"
            tickerRow.getAttribute("type").contains("index") -> "index"
            else -> tickerRow.getAttribute("type")
        }
        var board = tickerRow.getAttribute("marketprice_boardid")
        if (board.isEmpty())
            board = tickerRow.getAttribute("primary_boardid")
        val ticker = tickerRow.getAttribute("secid")
        return Triple(market, board, ticker)
    }

    private fun findData(document: Element, id: String): Element {
        for (i in 0 until document.getElementsByTagName("data").length) {
            val data = document.getElementsByTagName("data").item(i) as Element
            if (data.getAttribute("id") == id)
                return data
        }
        throw IllegalStateException("Element data $id not found")
    }

    private fun readXml(content: String): Document {

        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val xmlInput = InputSource(StringReader(content))

        return dBuilder.parse(xmlInput)
    }

    private fun currencyOf(name: String?): Currency {
        var curName = if (name == "SUR") "RUB" else name
        return Currency.values().find { c -> c.name == curName } ?: Currency.RUB
    }

}