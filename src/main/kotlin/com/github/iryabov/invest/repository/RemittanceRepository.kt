package com.github.iryabov.invest.repository

import com.github.iryabov.invest.entity.Remittance
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface RemittanceRepository: CrudRepository<Remittance, Long> {

}