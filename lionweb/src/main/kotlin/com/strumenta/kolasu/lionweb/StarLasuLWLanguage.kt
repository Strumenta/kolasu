package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.Multiplicity
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.Containment
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.language.LionCoreBuiltins
import io.lionweb.lioncore.java.language.PrimitiveType

object StarLasuLWLanguage : Language("com.strumenta.StarLasu") {
    init {
        val cleanedName = name.lowercase().lwIDCleanedVersion()
        id = "com-strumenta-StarLasu"
        key = "com_strumenta_starlasu"
        version = "1"
        val point =
            addConcept("Point").apply {
                addProperty("line", LionCoreBuiltins.getInteger(), Multiplicity.SINGULAR)
                addProperty("column", LionCoreBuiltins.getInteger(), Multiplicity.SINGULAR)
            }
        val position =
            addConcept("Range").apply {
                addContainment("start", point, Multiplicity.SINGULAR)
                addContainment("end", point, Multiplicity.SINGULAR)
            }
        addConcept("ASTNode").apply {
            addContainment("range", position, Multiplicity.OPTIONAL)
        }
        addPrimitiveType("Char")
    }

    val RangeStart: Containment
        get() = Range.getContainmentByName("start")!!

    val RangeEnd: Containment
        get() = Range.getContainmentByName("end")!!

    val Point: Concept
        get() = StarLasuLWLanguage.getConceptByName("Point")!!

    val Range: Concept
        get() = StarLasuLWLanguage.getConceptByName("Range")!!

    val ASTNodeRange: Containment
        get() = ASTNode.getContainmentByName("range")!!

    val ASTNode: Concept
        get() = StarLasuLWLanguage.getConceptByName("ASTNode")!!

    val char: PrimitiveType
        get() = StarLasuLWLanguage.getPrimitiveTypeByName("Char")!!
}
