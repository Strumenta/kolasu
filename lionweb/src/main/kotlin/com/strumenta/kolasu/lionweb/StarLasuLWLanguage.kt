package com.strumenta.kolasu.lionweb

import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.language.PrimitiveType

object StarLasuLWLanguage : Language() {
    init {
        name = "com.strumenta.StarLasu"
        id = "com_strumenta_starlasu"
        version = "1"
        key = id
        Concept(this, "ASTNode", id + "_ASTNode").setKey(id + "_ASTNode")
        PrimitiveType(this, "Char", id + "_Char").setKey(id + "_Char")
    }

    val ASTNode: Concept
        get() = getConceptByName("ASTNode")!!

    val char: PrimitiveType
        get() = getPrimitiveTypeByName("Char")!!
}
