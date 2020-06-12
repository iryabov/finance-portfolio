package com.github.iryabov.invest

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories

@SpringBootApplication
@EnableJdbcRepositories
class InvestApplication

fun main(args: Array<String>) {
    runApplication<InvestApplication>(*args)
}
