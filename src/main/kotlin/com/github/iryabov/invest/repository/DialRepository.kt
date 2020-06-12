package com.github.iryabov.invest.repository

import com.github.iryabov.invest.entity.Dial
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository


@Repository
interface DialRepository: CrudRepository<Dial, Long>  {
    @Modifying
    @Query("UPDATE dial SET active = NOT active WHERE id = :id")
    fun deactivate(@Param("id") id: Long): Boolean
}