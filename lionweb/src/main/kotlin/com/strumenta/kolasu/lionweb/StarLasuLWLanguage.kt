package com.strumenta.kolasu.lionweb

import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.ConceptInterface
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.language.LionCoreBuiltins
import io.lionweb.lioncore.java.language.Property

object StarLasuLWLanguage : Language() {
    init {
        setName("StarLasu")
        setID("com_strumenta_starlasu")
        setVersion("1")
        setKey(id)
        Concept(this, "ASTNode", id + "_ASTNode").setKey(id + "_ASTNode")
        ConceptInterface(this, "Named", id + "_Named").setKey(id + "_Named")
            .addFeature(
                Property.createRequired(
                    "name",
                    LionCoreBuiltins.getString(),
                    "starlasu_Named_name"
                ).setKey("starlasu_Named_name")
            )
    }

    val ASTNode: Concept
        get() = getConceptByName("ASTNode")!!

    val Named: ConceptInterface
        get() = getConceptInterfaceByName("Named")!!
}
