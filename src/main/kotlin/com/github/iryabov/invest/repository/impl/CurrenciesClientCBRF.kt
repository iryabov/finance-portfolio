package com.github.iryabov.invest.repository.impl

import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.repository.ExchangeRate
import com.github.iryabov.invest.repository.CurrenciesClient
import com.github.iryabov.invest.service.impl.invert
import org.springframework.http.MediaType
import org.springframework.stereotype.Repository
import org.springframework.web.reactive.function.client.WebClient
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory

@Repository
class CurrenciesClientCBRF : CurrenciesClient {
    private val client: WebClient = WebClient.create("http://www.cbr.ru/scripts/XML_daily.asp")

    override fun findCurrencyByDate(date: LocalDate): ExchangeRate {
        val dateStr = date.format(DateTimeFormatter.ofPattern("DD.MM.YYYY"))
        val response = client.get().uri("?date_req=${dateStr}")
                .accept(MediaType.APPLICATION_XML)
                .acceptCharset(Charsets.UTF_8)
                .retrieve().bodyToMono(String::class.java).block()
        val xml = readXml(response!!)
        val rates = Currency.values().filter { it != Currency.RUB }.map { it to findValue(xml, it).invert() }.toMap()
        return ExchangeRate(date = date, base = Currency.RUB, rates = rates)
    }

    private fun readXml(content: String): Document {

        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val xmlInput = InputSource(StringReader(content))

        return dBuilder.parse(xmlInput)
    }

    private fun findValue(doc: Document, currency: Currency): BigDecimal {
        val valutes = doc.getElementsByTagName("Valute")
        for (i in 0 until valutes.length) {
            val valute = (valutes.item(i) as Element)
            val charCode = valute.getElementsByTagName("CharCode").item(0).textContent
            if (charCode == currency.name) {
                val value = valute.getElementsByTagName("Value").item(0).textContent
                return BigDecimal(value.replace(",", "."))
            }
        }
        return BigDecimal.ONE
    }
}