package com.strumenta.kolasu.language

val intType = PrimitiveType.get("kotlin.Int")
val stringType = PrimitiveType.get("kotlin.String")
val booleanType = PrimitiveType.get("kotlin.Boolean")
val charType = PrimitiveType.get("kotlin.Char")

val builtinStarLasuTypes = setOf(intType, stringType, booleanType, charType)

object BaseStarLasuLanguage : StarLasuLanguage("com.strumenta.basestarlasulanguage") {
    init {
        types.addAll(builtinStarLasuTypes)
        val baseStarLasuConcept = Concept(this, "ASTNode")
        types.add(baseStarLasuConcept)
        val iNamed = ConceptInterface(this, "INamed")
        types.add(iNamed)
        val primitiveType = PrimitiveType("Range")
        types.add(primitiveType)
        val point = PrimitiveType("Point")
        types.add(point)
    }

    val astNode: Concept
        get() = getConcept("ASTNode")

    val iNamed: ConceptInterface
        get() = getConceptInterface("INamed")

    val range: PrimitiveType
        get() = getPrimitiveType("Range")

    val point: PrimitiveType
        get() = getPrimitiveType("Point")
}
