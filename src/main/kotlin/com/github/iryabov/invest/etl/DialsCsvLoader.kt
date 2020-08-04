package com.github.iryabov.invest.etl

import com.github.iryabov.invest.model.AccountForm
import com.github.iryabov.invest.model.DialForm
import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.relation.DialType
import com.github.iryabov.invest.repository.AccountRepository
import com.github.iryabov.invest.repository.DialRepository
import com.github.iryabov.invest.service.InvestService
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.util.*
import com.github.iryabov.invest.etl.CsvColumn.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class DialsCsvLoader(
        val dialRepo: DialRepository,
        val accountRepo: AccountRepository,
        val service: InvestService
) {
    private val delimiter = ","
    private val skipHeader = true

    fun load(csv: Resource) {
        val records = readRecords(csv)
        for (record in records) {
            val dial = DialForm(
                    ticker = record[TICKER.idx].toTicker(),
                    opened = record[OPENED.idx].toDate(),
                    currency = record[CURRENCY.idx].toCurrency(),
                    volume = record[AMOUNT.idx].toAmount(),
                    quantity = record[QUANTITY.idx].toQuantity(),
                    type = record[TYPE.idx].toType())
            service.addDial(record[ACCOUNT.idx].toAccountId(), dial)
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
            while (row.hasNext()) {
                values.add(row.next())
            }
        }
        return values
    }

    private fun String.toAccountId(): Int {
        return accountRepo.findByName(this)?.id ?: service.createAccount(AccountForm(name = this))
    }

    private fun String.toCurrency(): Currency {
        return when (this) {
            "р" -> Currency.RUB
            "$" -> Currency.USD
            "e" -> Currency.EUR
            else -> throw IllegalArgumentException(this)
        }
    }

    private fun String.toDate(): LocalDate {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yy")
        return LocalDate.parse(this, formatter)
    }

    private fun String.toType(): DialType {
        return when (this) {
            "b" -> DialType.PURCHASE
            "s" -> DialType.SALE
            "d" -> DialType.DIVIDEND
            "i" -> DialType.DEPOSIT
            "o" -> DialType.WITHDRAWALS
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
                    .toBigDecimal()
        else BigDecimal.ZERO
    }

    private fun String.toQuantity(): Int {
        return if (!this.isBlank())
            this.replace(" ", "")
                    .replace("\"", "")
                    .replace(" ", "").toInt()
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