package com.github.iryabov.invest.ui

import net.n2oapp.framework.api.metadata.Compiled
import net.n2oapp.framework.api.metadata.compile.BindProcessor
import net.n2oapp.framework.api.metadata.meta.ModelLink
import net.n2oapp.framework.api.metadata.meta.widget.form.Form
import net.n2oapp.framework.config.metadata.compile.BaseMetadataBinder
import org.springframework.stereotype.Component

@Component
class FormBinder: BaseMetadataBinder<Form> {
    override fun bind(form: Form, p: BindProcessor): Form {
        if (form.formDataProvider != null) {
            val pathMapping: MutableMap<String, ModelLink> = form.formDataProvider.pathMapping
            val queryMapping: MutableMap<String, ModelLink> = form.formDataProvider.queryMapping
            form.formDataProvider.url = p.resolveUrl(form.formDataProvider.url, pathMapping, queryMapping)
            pathMapping.forEach { (k: String, v: ModelLink?) -> pathMapping[k] = p.resolveLink(v) as ModelLink }
            queryMapping.forEach { (k: String, v: ModelLink?) -> queryMapping[k] = p.resolveLink(v) as ModelLink }
        }
        return form
    }

    override fun getCompiledClass(): Class<out Compiled> {
        return Form::class.java
    }
}