package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.Multiplicity
import com.strumenta.kolasu.validation.IssueSeverity
import com.strumenta.kolasu.validation.IssueType
import io.lionweb.lioncore.java.language.Annotation
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.Interface
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.language.LionCoreBuiltins
import io.lionweb.lioncore.java.language.PrimitiveType
import io.lionweb.lioncore.java.language.Property
import io.lionweb.lioncore.java.language.Reference
import io.lionweb.lioncore.java.self.LionCore

private const val PLACEHOLDER_NODE = "PlaceholderNode"

object StarLasuLWLanguage : Language("com.strumenta.StarLasu") {

    val CommonElement: Interface
    val Issue: Concept
    val ParsingResult: Concept

    init {
        id = "com-strumenta-StarLasu"
        key = "com_strumenta_starlasu"
        version = "1"
        addPrimitiveType("Char")
        addPrimitiveType("Point")
        val position = addPrimitiveType("Position")
        val astNode = addConcept("ASTNode").apply {
            addProperty("position", position, Multiplicity.OPTIONAL)
        }
        astNode.addReference("originalNode", astNode, Multiplicity.OPTIONAL)
        astNode.addReference("transpiledNode", astNode, Multiplicity.MANY)

        addPlaceholderNodeAnnotation(astNode)

        CommonElement = addInterface("CommonElement")
        addInterface("BehaviorDeclaration").apply { addExtendedInterface(CommonElement) }
        addInterface("Documentation").apply { addExtendedInterface(CommonElement) }
        addInterface("EntityDeclaration").apply { addExtendedInterface(CommonElement) }
        addInterface("EntityGroupDeclaration").apply { addExtendedInterface(CommonElement) }
        addInterface("Expression").apply { addExtendedInterface(CommonElement) }
        addInterface("Parameter").apply { addExtendedInterface(CommonElement) }
        addInterface("PlaceholderElement").apply { addExtendedInterface(CommonElement) }
        addInterface("Statement").apply { addExtendedInterface(CommonElement) }
        addInterface("TypeAnnotation").apply { addExtendedInterface(CommonElement) }

        Issue = addConcept("Issue").apply {
            addProperty("type", addEnumerationFromClass(this@StarLasuLWLanguage, IssueType::class))
            addProperty("message", LionCoreBuiltins.getString())
            addProperty("severity", addEnumerationFromClass(this@StarLasuLWLanguage, IssueSeverity::class))
            addProperty("position", position, Multiplicity.OPTIONAL)
        }

        ParsingResult = addConcept("ParsingResult").apply {
            addContainment("issues", Issue, Multiplicity.MANY)
            addContainment("root", ASTNode, Multiplicity.OPTIONAL)
            addProperty("code", LionCoreBuiltins.getString(), Multiplicity.OPTIONAL)
        }
    }

    private fun addPlaceholderNodeAnnotation(astNode: Concept) {
        val placeholderNodeAnnotation = Annotation(
            this,
            PLACEHOLDER_NODE,
            idForContainedElement(PLACEHOLDER_NODE),
            keyForContainedElement(PLACEHOLDER_NODE)
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

    val Position: PrimitiveType
        get() = StarLasuLWLanguage.getPrimitiveTypeByName("Position")!!

    val ASTNodePosition: Property
        get() = ASTNode.getPropertyByName("position")!!

    val ASTNodeOriginalNode: Reference
        get() = ASTNode.getReferenceByName("originalNode")!!

    val ASTNodeTranspiledNodes: Reference
        get() = ASTNode.getReferenceByName("transpiledNode")!!

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

}
