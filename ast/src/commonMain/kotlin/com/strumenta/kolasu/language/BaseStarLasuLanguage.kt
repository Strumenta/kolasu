package com.strumenta.kolasu.language

import com.strumenta.kolasu.model.BehaviorDeclaration
import com.strumenta.kolasu.model.CommonElement
import com.strumenta.kolasu.model.Documentation
import com.strumenta.kolasu.model.EntityDeclaration
import com.strumenta.kolasu.model.EntityGroupDeclaration
import com.strumenta.kolasu.model.Expression
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.Parameter
import com.strumenta.kolasu.model.Statement
import com.strumenta.kolasu.model.TypeAnnotation

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

        val commonElement = ConceptInterface(this, CommonElement::class.simpleName!!)
        types.add(commonElement)

        setOf(
            Expression::class, Statement::class, EntityDeclaration::class, BehaviorDeclaration::class,
            Parameter::class, Documentation::class, EntityGroupDeclaration::class, TypeAnnotation::class
        ).forEach { kClass ->
            val aCommonElement = ConceptInterface(this, CommonElement::class.simpleName!!)
            aCommonElement.superInterfaces.add(commonElement)
            types.add(aCommonElement)
        }

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

    val expression: ConceptInterface
        get() = getConceptInterface(Expression::class.simpleName!!)
}
