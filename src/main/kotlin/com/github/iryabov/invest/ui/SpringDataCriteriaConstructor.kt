package com.github.iryabov.invest.ui

import net.n2oapp.framework.api.criteria.N2oPreparedCriteria
import net.n2oapp.framework.engine.data.N2oCriteriaConstructor
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

@Suppress("UNCHECKED_CAST")
class SpringDataCriteriaConstructor: N2oCriteriaConstructor(true) {
    override fun construct(criteria: N2oPreparedCriteria?, instance: Any?): Any? {
        return if (instance is Pageable) {
            PageRequest.of(criteria!!.page - 1, criteria.size)
        } else
            super.construct(criteria, instance)
    }


}