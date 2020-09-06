package com.github.iryabov.invest.model

import com.github.iryabov.invest.relation.AssetClass
import com.github.iryabov.invest.relation.Country
import com.github.iryabov.invest.relation.Currency
import com.github.iryabov.invest.relation.Sector

data class SecurityCriteria(
        var assetClass: AssetClass? = null,
        var sector: Sector? = null,
        var currency: Currency? = null,
        var country: Country? = null,
        var accountId: Int? = null
)