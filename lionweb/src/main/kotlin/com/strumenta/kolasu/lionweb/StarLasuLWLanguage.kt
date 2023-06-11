package com.strumenta.kolasu.lionweb

import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.Language

object StarLasuLWLanguage : Language() {
    init {
        Concept(this, "ASTNode")
    }

    val ASTNode: Concept
        get() = getConceptByName("ASTNode")!!
}