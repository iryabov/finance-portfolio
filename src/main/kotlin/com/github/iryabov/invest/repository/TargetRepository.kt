package com.github.iryabov.invest.repository

import com.github.iryabov.invest.entity.Target
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TargetRepository: CrudRepository<Target, Long> {
}