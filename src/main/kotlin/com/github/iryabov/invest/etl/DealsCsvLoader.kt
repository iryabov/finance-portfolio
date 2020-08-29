package com.github.iryabov.invest.etl

import com.github.iryabov.invest.model.AccountForm
import com.github.iryabov.invest.model.DealForm
import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.relation.DealType
import com.github.iryabov.invest.repository.AccountRepository
import com.github.iryabov.invest.repository.DealRepository
import com.github.iryabov.invest.service.InvestService
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.util.*
import com.github.iryabov.invest.etl.CsvColumn.*
import com.github.iryabov.invest.service.impl.round
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Component
class DealsCsvLoader(
        val dealRepo: DealRepository,
        val accountRepo: AccountRepository,
        val service: InvestService
) {
    private val delimiter = ","
    private val skipHeader = true

    fun load(csv: Resource) {
        val records = readRecords(csv)
        for (record in records) {
            try {
                val dial = DealForm(
                        ticker = record[TICKER.idx].toTicker(),
                        opened = record[OPENED.idx].toDate(),
                        currency = record[CURRENCY.idx].toCurrency(),
                        volume = record[AMOUNT.idx].toAmount(),
                        quantity = if (record.size > QUANTITY.idx) record[QUANTITY.idx].toQuantity() else null,
                        type = record[TYPE.idx].toType())
                service.addDeal(record[ACCOUNT.idx].toAccountId(), dial)
            } catch (e: Exception) {
                print(record)
                throw e
            }
        }
    }

    private fun readRecords(csv: Resource): List<List<String>> {
        val records: MutableList<List<String>> = ArrayList()
        Scanner(csv.inputStream).use { lines ->
            if (skipHeader && lines.hasNextLine())
                lines.nextLine()
            while (lines.hasNextLine()) {
                records.add(readRecordFromLine(lines.nextLine()))
            }
        }
        return records
    }

    private fun readRecordFromLine(line: String): List<String> {
        val values: MutableList<String> = ArrayList()
        Scanner(line).use { row ->
            row.useDelimiter(delimiter)
            var part: String? = null
            while (row.hasNext()) {
                val cur = row.next().trim()
                if (part != null && cur.endsWith("\"") && !cur.startsWith("\"")) {
                    values.add("$part,$cur")
                    part = null
                } else if (part == null && cur.startsWith("\"") && !cur.endsWith("\"")) {
                    part = cur
                } else {
                    values.add(cur)
                }
            }
        }
        return values
    }

    private fun String.toAccountId(): Int {
        return accountRepo.findByName(this)?.id ?: service.createAccount(AccountForm(name = this))
    }

    private fun String.toCurrency(): Currency {
        return when (this) {
            "р", "RUB" -> Currency.RUB
            "$", "USD" -> Currency.USD
            "e", "EUR" -> Currency.EUR
            else -> throw IllegalArgumentException(this)
        }
    }

    private fun String.toDate(): LocalDate {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        return LocalDate.parse(this, formatter)
    }

    private fun String.toType(): DealType {
        return when (this) {
            "b", "PURCHASE" -> DealType.PURCHASE
            "s", "SALE" -> DealType.SALE
            "d", "DIVIDEND" -> DealType.DIVIDEND
            "p", "PERCENT" -> DealType.PERCENT
            "t", "TAX" -> DealType.TAX
            "i", "DEPOSIT" -> DealType.DEPOSIT
            "o", "WITHDRAWALS" -> DealType.WITHDRAWALS
            else -> throw java.lang.IllegalArgumentException(this)
        }
    }

    private fun String.toTicker(): String {
        return this
    }

    private fun String.toAmount(): BigDecimal {
        return if (!this.isBlank())
            this.replace(" ", "")
                    .replace("\"", "")
                    .replace(" ", "")
                    .replace(",", ".")
                    .toBigDecimal().abs()
        else BigDecimal.ZERO
    }

    private fun String.toQuantity(): Int {
        return if (!this.isBlank())
            abs(this.replace(" ", "")
                    .replace("\"", "")
                    .replace(" ", "")
                    .replace(",", ".")
                    .toBigDecimal().round(0).toInt())
        else 0
    }

}

enum class CsvColumn(val idx: Int) {
    TYPE(0),
    OPENED(1),
    ACCOUNT(2),
    TICKER(3),
    CURRENCY(4),
    AMOUNT(5),
    QUANTITY(6)
}