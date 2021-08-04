package com.strumenta.kolasu.emf

import org.eclipse.emf.ecore.resource.Resource

interface EMFEnabledParser {
    fun generateMetamodel(resource: Resource)
}

abstract class AbstractEMFEnabledParser : EMFEnabledParser {
    override fun generateMetamodel(resource: Resource) {
        resource.contents.add(KOLASU_METAMODEL)
        doGenerateMetamodel(resource)
    }

    abstract fun doGenerateMetamodel(resource: Resource)
}
