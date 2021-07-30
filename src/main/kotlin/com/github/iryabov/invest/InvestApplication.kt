package com.github.iryabov.invest

import com.github.iryabov.invest.ui.SpringDataCriteriaConstructor
import net.n2oapp.framework.api.MetadataEnvironment
import net.n2oapp.framework.api.data.CriteriaConstructor
import net.n2oapp.framework.api.data.QueryExceptionHandler
import net.n2oapp.framework.api.data.QueryProcessor
import net.n2oapp.framework.engine.data.N2oCriteriaConstructor
import net.n2oapp.framework.engine.data.N2oInvocationFactory
import net.n2oapp.framework.engine.data.N2oQueryProcessor
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories

@SpringBootApplication
@EnableJdbcRepositories
class InvestApplication {
    @Bean
    fun queryProcessor(invocationFactory: N2oInvocationFactory?,
                       exceptionHandler: QueryExceptionHandler?,
                       environment: MetadataEnvironment?): QueryProcessor? {
        val n2oQueryProcessor = N2oQueryProcessor(invocationFactory, exceptionHandler)
        n2oQueryProcessor.setCriteriaResolver(SpringDataCriteriaConstructor())
        n2oQueryProcessor.setPageStartsWith0(true)
        n2oQueryProcessor.setEnvironment(environment)
        return n2oQueryProcessor
    }
}

fun main(args: Array<String>) {
    runApplication<InvestApplication>(*args)
}
