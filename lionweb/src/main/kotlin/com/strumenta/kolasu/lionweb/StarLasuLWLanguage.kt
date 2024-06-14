package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.Multiplicity
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.language.PrimitiveType
import io.lionweb.lioncore.java.language.Property
import io.lionweb.lioncore.java.language.Reference

object StarLasuLWLanguage : Language("com.strumenta.StarLasu") {
    init {
        id = "com-strumenta-StarLasu"
        key = "com_strumenta_starlasu"
        version = "1"
        addPrimitiveType("Char")
        addPrimitiveType("Point")
        val range =
            addPrimitiveType("Range")
        val astNode =
            addConcept("ASTNode").apply {
                addProperty("range", range, Multiplicity.OPTIONAL)
            }
        astNode.addReference("originalNode", astNode, Multiplicity.OPTIONAL)
        astNode.addReference("transpiledNode", astNode, Multiplicity.MANY)
    }

    val Point: PrimitiveType
        get() = StarLasuLWLanguage.getPrimitiveTypeByName("Point")!!

    val Range: PrimitiveType
        get() = StarLasuLWLanguage.getPrimitiveTypeByName("Range")!!

    val ASTNodeRange: Property
        get() = ASTNode.getPropertyByName("range")!!

    val ASTNodeOriginalNode: Reference
        get() = ASTNode.getReferenceByName("originalNode")!!

    val ASTNodeTranspiledNodes: Reference
        get() = ASTNode.getReferenceByName("transpiledNode")!!

    val ASTNode: Concept
        get() = StarLasuLWLanguage.getConceptByName("ASTNode")!!

    val char: PrimitiveType
        get() = StarLasuLWLanguage.getPrimitiveTypeByName("Char")!!
}
