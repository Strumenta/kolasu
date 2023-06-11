package com.strumenta.kolasu.lionweb

import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.ConceptInterface
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.language.LionCoreBuiltins
import io.lionweb.lioncore.java.language.Property

object StarLasuLWLanguage : Language() {
    init {
        Concept(this, "ASTNode")
        ConceptInterface(this, "Named")
            .addFeature(Property.createRequired("name", LionCoreBuiltins.getString()))
    }

    val ASTNode: Concept
        get() = getConceptByName("ASTNode")!!

    val Named: ConceptInterface
        get() = getConceptInterfaceByName("Named")!!
}