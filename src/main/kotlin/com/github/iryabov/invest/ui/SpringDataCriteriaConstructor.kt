package com.github.iryabov.invest.ui

import net.n2oapp.framework.api.criteria.N2oPreparedCriteria
import net.n2oapp.framework.engine.data.N2oCriteriaConstructor
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

@Suppress("UNCHECKED_CAST")
class SpringDataCriteriaConstructor: N2oCriteriaConstructor(true) {
    override fun <T : Any?> construct(criteria: N2oPreparedCriteria, criteriaClass: Class<T>): T {
        return if (criteriaClass.isAssignableFrom(Pageable::class.java)) {
            PageRequest.of(criteria.page - 1, criteria.size) as T
        } else
            super.construct(criteria, criteriaClass)
    }
}