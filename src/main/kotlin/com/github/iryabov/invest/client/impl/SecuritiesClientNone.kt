package com.github.iryabov.invest.client.impl

import com.github.iryabov.invest.client.SecuritiesClient
import com.github.iryabov.invest.client.Security
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class SecuritiesClientNone: SecuritiesClient {
    override fun findHistoryPrices(name: String, from: LocalDate, till: LocalDate): List<Security> = ArrayList()
    override fun findLastPrice(name: String): Security = Security(date = LocalDate.now(), ticker = name)
    override fun findByName(name: String): List<Security> = ArrayList()
}