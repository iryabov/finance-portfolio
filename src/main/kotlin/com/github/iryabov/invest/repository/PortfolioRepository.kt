package com.github.iryabov.invest.repository

import com.github.iryabov.invest.entity.Portfolio
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PortfolioRepository: CrudRepository<Portfolio, Int> {
}