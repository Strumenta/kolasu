package com.strumenta.kolasu.lionweb

import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.Language

object StarLasuLWLanguage : Language() {
    init {
        name = "com.strumenta.StarLasu"
        id = "com_strumenta_starlasu"
        version = "1"
        key = id
        Concept(this, "ASTNode", id + "_ASTNode").setKey(id + "_ASTNode")
    }

    val ASTNode: Concept
        get() = getConceptByName("ASTNode")!!
}
