package com.github.iryabov.invest.repository

import com.github.iryabov.invest.entity.Account
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountRepository: CrudRepository<Account, Int> {

    fun findByName(name: String): Account?
}