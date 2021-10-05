package com.strumenta.kolasu.emf

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.parsing.KolasuParser
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.ParserRuleContext
import org.eclipse.emf.ecore.resource.Resource

interface EMFMetamodelSupport {
    fun generateMetamodel(resource: Resource)
}

abstract class EMFEnabledParser<R : Node, P : Parser, C : ParserRuleContext> : KolasuParser<R, P, C>(),
    EMFMetamodelSupport {
    override fun generateMetamodel(resource: Resource) {
        resource.contents.add(KOLASU_METAMODEL)
        doGenerateMetamodel(resource)
    }

    abstract fun doGenerateMetamodel(resource: Resource)
}
