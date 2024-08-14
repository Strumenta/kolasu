package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.Multiplicity
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Position
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
import io.lionweb.lioncore.java.serialization.PrimitiveValuesSerialization.PrimitiveDeserializer
import io.lionweb.lioncore.java.serialization.PrimitiveValuesSerialization.PrimitiveSerializer
import io.lionweb.lioncore.kotlin.MetamodelRegistry

private const val PLACEHOLDER_NODE = "PlaceholderNode"

/**
 * When this object is referenced the initialization is performed. When that happens the serializers and deserializers
 * for Position and other primitive types are registered in the MetamodelRegistry.
 */
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
        astNode.addReference("transpiledNodes", astNode, Multiplicity.MANY)

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
        registerSerializersAndDeserializersInMetamodelRegistry()
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
        get() = ASTNode.getReferenceByName("transpiledNodes")!!

    val ASTNode: Concept
        get() = StarLasuLWLanguage.getConceptByName("ASTNode")!!

    val Char: PrimitiveType
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

private fun registerSerializersAndDeserializersInMetamodelRegistry() {
    val charSerializer = PrimitiveSerializer<Char> { value -> "$value" }
    val charDeserializer = PrimitiveDeserializer<Char> { serialized ->
        require(serialized.length == 1)
        serialized[0]
    }
    MetamodelRegistry.addSerializerAndDeserializer(StarLasuLWLanguage.Char, charSerializer, charDeserializer)

    val pointSerializer: PrimitiveSerializer<Point> =
        PrimitiveSerializer<Point> { value ->
            if (value == null) {
                return@PrimitiveSerializer null
            }
            "L${value.line}:${value.column}"
        }
    val pointDeserializer: PrimitiveDeserializer<Point> =
        PrimitiveDeserializer<Point> { serialized ->
            if (serialized == null) {
                return@PrimitiveDeserializer null
            }
            require(serialized.startsWith("L"))
            require(serialized.removePrefix("L").isNotEmpty())
            val parts = serialized.removePrefix("L").split(":")
            require(parts.size == 2)
            Point(parts[0].toInt(), parts[1].toInt())
        }
    MetamodelRegistry.addSerializerAndDeserializer(StarLasuLWLanguage.Point, pointSerializer, pointDeserializer)

    val positionSerializer = PrimitiveSerializer<Position> { value ->
        "${pointSerializer.serialize((value as Position).start)}-${pointSerializer.serialize(value.end)}"
    }
    val positionDeserializer = PrimitiveDeserializer<Position> { serialized ->
        if (serialized == null) {
            null
        } else {
            val parts = serialized.split("-")
            require(parts.size == 2)
            Position(pointDeserializer.deserialize(parts[0]), pointDeserializer.deserialize(parts[1]))
        }
    }
    MetamodelRegistry.addSerializerAndDeserializer(
        StarLasuLWLanguage.Position,
        positionSerializer,
        positionDeserializer
    )
}
