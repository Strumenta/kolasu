package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.Multiplicity
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.language.PrimitiveType
import io.lionweb.lioncore.java.language.Property

object StarLasuLWLanguage : Language("com.strumenta.StarLasu") {
    init {
        val cleanedName = name.lowercase().lwIDCleanedVersion()
        id = "com-strumenta-StarLasu"
        key = "com_strumenta_starlasu"
        version = "1"
        val point =
            addPrimitiveType("Point")
        val range =
            addPrimitiveType("Range")
        addConcept("ASTNode").apply {
            addProperty("range", range, Multiplicity.OPTIONAL)
        }
        addPrimitiveType("Char")
    }

    val Point: PrimitiveType
        get() = StarLasuLWLanguage.getPrimitiveTypeByName("Point")!!

    val Range: PrimitiveType
        get() = StarLasuLWLanguage.getPrimitiveTypeByName("Range")!!

    val ASTNodeRange: Property
        get() = ASTNode.getPropertyByName("range")!!

    val ASTNode: Concept
        get() = StarLasuLWLanguage.getConceptByName("ASTNode")!!

    val char: PrimitiveType
        get() = StarLasuLWLanguage.getPrimitiveTypeByName("Char")!!
}
