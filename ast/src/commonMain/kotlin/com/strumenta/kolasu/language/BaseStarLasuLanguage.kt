package com.strumenta.kolasu.language

import com.strumenta.kolasu.model.NodeLike

val intType = PrimitiveType.get("kotlin.Int")
val stringType = PrimitiveType.get("kotlin.String")
val booleanType = PrimitiveType.get("kotlin.Boolean")
val floatType = PrimitiveType.get("kotlin.Float")
val charType = PrimitiveType.get("kotlin.Char")

val builtinStarLasuTypes = setOf(intType, stringType, booleanType, charType, floatType)

object BaseStarLasuLanguage : StarLasuLanguage("com.strumenta.basestarlasulanguage") {
    init {
        types.addAll(builtinStarLasuTypes)
        val baseStarLasuConcept = Concept(this, "ASTNode")
        baseStarLasuConcept.correspondingKotlinClass = NodeLike::class
        types.add(baseStarLasuConcept)
        val iPossiblyNamed = ConceptInterface(this, "IPossiblyNamed")
        iPossiblyNamed.declaredFeatures.add(Property("name", true, stringType, { TODO() }))
        types.add(iPossiblyNamed)
        val iNamed = ConceptInterface(this, "INamed")
        iNamed.superInterfaces.add(iPossiblyNamed)
        iNamed.declaredFeatures.add(Property("name", false, stringType, { TODO() }))
        types.add(iNamed)
        val primitiveType = PrimitiveType("Range")
        types.add(primitiveType)
        val point = PrimitiveType("Point")
        types.add(point)
    }

    val astNode: Concept
        get() = getConcept("ASTNode")

    val iPossiblyNamed: ConceptInterface
        get() = getConceptInterface("IPossiblyNamed")

    val iNamed: ConceptInterface
        get() = getConceptInterface("INamed")

    val range: PrimitiveType
        get() = getPrimitiveType("Range")

    val point: PrimitiveType
        get() = getPrimitiveType("Point")
}
