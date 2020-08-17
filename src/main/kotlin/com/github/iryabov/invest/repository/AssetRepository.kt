package com.github.iryabov.invest.repository

import com.github.iryabov.invest.entity.Asset
import com.github.iryabov.invest.model.SecurityView
import com.github.iryabov.invest.relation.Currency
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface AssetRepository: PagingAndSortingRepository<Asset, String> {
}