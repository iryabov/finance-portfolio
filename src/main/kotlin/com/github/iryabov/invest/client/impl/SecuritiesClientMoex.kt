package com.github.iryabov.invest.client.impl

import com.github.iryabov.invest.client.Security
import com.github.iryabov.invest.client.SecuritiesClient
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
    private val client: WebClient = WebClient.create("https://iss.moex.com/iss")

    override fun findHistoryPrices(name: String, from: LocalDate, till: LocalDate): List<Security> {
        val (market, board, ticker) = findMarketAndBoard(name)
        val fromStr = from.format(DateTimeFormatter.ISO_DATE)
        val tillStr = till.format(DateTimeFormatter.ISO_DATE)
        val response = callCandlebordes(market, board, ticker, fromStr, tillStr)
        val rows = readRows(response, "history")
        if (rows == null || rows.length == 0)
            throw IllegalStateException("Security $ticker not found")
        val list =  ArrayList<Security>()
        for (i in 0 until rows.length) {
            val row = rows.item(i) as Element
            list.add(Security(
                    date = LocalDate.parse(row.getAttribute("TRADEDATE"), DateTimeFormatter.ISO_DATE),
                    ticker = row.getAttribute("SECID"),
                    shortName = row.getAttribute("SHORTNAME"),
                    price = BigDecimal(row.getAttribute("CLOSE"))
            ))
        }
        return list
    }

    override fun findLastPrice(name: String): Security {
        val (market, board, ticker) = findMarketAndBoard(name)
        val response = callTicker(market, board, ticker)
        val rows = readRows(response)
        if (rows == null || rows.length == 0)
            throw IllegalStateException("Security $ticker not found")
        val row0 = rows.item(0) as Element
        assert(row0.getAttribute("SECID") == ticker)
        return Security(
                date = LocalDate.parse(row0.getAttribute("PREVDATE"), DateTimeFormatter.ISO_DATE),
                ticker = row0.getAttribute("SECID"),
                shortName = row0.getAttribute("SHORTNAME"),
                price = BigDecimal(row0.getAttribute("PREVADMITTEDQUOTE")))
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
                    ticker = row.getAttribute("secid"),
                    shortName = row.getAttribute("shortname"),
                    fullName = row.getAttribute("name"))
            )
        }
        return list
    }

    private fun callCandlebordes(market: String, board: String, ticker: String, fromStr: String?, tillStr: String?): String? {
        val response = client.get().uri("/history/engines/stock/markets/$market/boards/$board/securities/$ticker/candlebordes.xml?from=$fromStr&till=$tillStr&iss.meta=off")
                .accept(MediaType.APPLICATION_XML)
                .acceptCharset(Charsets.UTF_8)
                .retrieve().bodyToMono(String::class.java).block()
        return response
    }

    private fun callTicker(market: String, board: String, ticker: String): String? {
        val response = client.get().uri("/engines/stock/markets/$market/boards/$board/securities/$ticker.xml?iss.meta=off")
                .accept(MediaType.APPLICATION_XML)
                .acceptCharset(Charsets.UTF_8)
                .retrieve().bodyToMono(String::class.java).block()
        return response
    }

    private fun callSecurities(name: String): String? {
        val response = client.get().uri("/securities.xml?q=$name&iss.meta=off")
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
        val tickerRow = tickerRows?.item(0) as Element
        val market = when {
            tickerRow.getAttribute("type").contains("share") -> "shares"
            tickerRow.getAttribute("type").contains("bond") -> "bonds"
            else -> "shares"
        }
        val board = tickerRow.getAttribute("marketprice_boardid")
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

}