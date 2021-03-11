package com.github.iryabov.invest.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded

data class PortfolioAccount(
        @Id
        @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL)
        val id: PortfolioAccountID,
        val accountName: String? = null
)

data class PortfolioAccountID(
        val portfolioId: Int,
        val accountId: Int
)