package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.Multiplicity
import io.lionweb.lioncore.java.language.Annotation
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.Interface
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.language.PrimitiveType
import io.lionweb.lioncore.java.language.Property
import io.lionweb.lioncore.java.language.Reference
import io.lionweb.lioncore.java.self.LionCore

private const val PLACEHOLDER_NODE = "PlaceholderNode"

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
        astNode.addReference("transpiledNodes", astNode, Multiplicity.MANY)

        addPlaceholderNodeAnnotation(astNode)

        val commonElement = addInterface("CommonElement")
        addInterface("BehaviorDeclaration").apply { addExtendedInterface(commonElement) }
        addInterface("Documentation").apply { addExtendedInterface(commonElement) }
        addInterface("EntityDeclaration").apply { addExtendedInterface(commonElement) }
        addInterface("EntityGroupDeclaration").apply { addExtendedInterface(commonElement) }
        addInterface("Expression").apply { addExtendedInterface(commonElement) }
        addInterface("Parameter").apply { addExtendedInterface(commonElement) }
        addInterface("PlaceholderElement").apply { addExtendedInterface(commonElement) }
        addInterface("Statement").apply { addExtendedInterface(commonElement) }
        addInterface("TypeAnnotation").apply { addExtendedInterface(commonElement) }
    }

    private fun addPlaceholderNodeAnnotation(astNode: Concept) {
        val placeholderNodeAnnotation =
            Annotation(
                this,
                PLACEHOLDER_NODE,
                idForContainedElement(PLACEHOLDER_NODE),
                keyForContainedElement(PLACEHOLDER_NODE),
            )
        placeholderNodeAnnotation.annotates = LionCore.getConcept()
        val reference =
            Reference().apply {
                this.name = "originalNode"
                this.id = "${placeholderNodeAnnotation.id!!.removeSuffix("-id")}-$name-id"
                this.key = "${placeholderNodeAnnotation.key!!.removeSuffix("-key")}-$name-key"
                this.type = astNode
                this.setOptional(true)
                this.setMultiple(false)
            }
        placeholderNodeAnnotation.addFeature(reference)
        addElement(placeholderNodeAnnotation)
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
        get() = ASTNode.getReferenceByName("transpiledNodes")!!

    val ASTNode: Concept
        get() = StarLasuLWLanguage.getConceptByName("ASTNode")!!

    val char: PrimitiveType
        get() = StarLasuLWLanguage.getPrimitiveTypeByName("Char")!!

    val PlaceholderNode: Annotation
        get() = StarLasuLWLanguage.elements.filterIsInstance<Annotation>().find { it.name == PLACEHOLDER_NODE }!!

    val PlaceholderNodeOriginalNode: Reference
        get() = PlaceholderNode.getReferenceByName("originalNode")!!

    val BehaviorDeclaration: Interface
        get() = StarLasuLWLanguage.getInterfaceByName("BehaviorDeclaration")!!
    val Documentation: Interface
        get() = StarLasuLWLanguage.getInterfaceByName("Documentation")!!
    val EntityDeclaration: Interface
        get() = StarLasuLWLanguage.getInterfaceByName("EntityDeclaration")!!
    val EntityGroupDeclaration: Interface
        get() = StarLasuLWLanguage.getInterfaceByName("EntityGroupDeclaration")!!
    val Expression: Interface
        get() = StarLasuLWLanguage.getInterfaceByName("Expression")!!
    val Parameter: Interface
        get() = StarLasuLWLanguage.getInterfaceByName("Parameter")!!
    val PlaceholderElement: Interface
        get() = StarLasuLWLanguage.getInterfaceByName("PlaceholderElement")!!
    val Statement: Interface
        get() = StarLasuLWLanguage.getInterfaceByName("Statement")!!
    val TypeAnnotation: Interface
        get() = StarLasuLWLanguage.getInterfaceByName("TypeAnnotation")!!
    val CommonElement: Interface
        get() = StarLasuLWLanguage.getInterfaceByName("CommonElement")!!
}
