package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.parsing.KolasuToken
import com.strumenta.kolasu.parsing.TokenCategory
import com.strumenta.kolasu.validation.IssueSeverity
import com.strumenta.kolasu.validation.IssueType
import com.strumenta.starlasu.base.ASTLanguage
import io.lionweb.lioncore.java.LionWebVersion
import io.lionweb.lioncore.java.language.Annotation
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.Enumeration
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
import io.lionweb.lioncore.kotlin.Multiplicity
import io.lionweb.lioncore.kotlin.addLiteral
import io.lionweb.lioncore.kotlin.createConcept
import io.lionweb.lioncore.kotlin.createContainment
import io.lionweb.lioncore.kotlin.createInterface
import io.lionweb.lioncore.kotlin.createPrimitiveType
import io.lionweb.lioncore.kotlin.createProperty
import io.lionweb.lioncore.kotlin.createReference
import com.strumenta.kolasu.model.BehaviorDeclaration as KBehaviorDeclaration
import com.strumenta.kolasu.model.CommonElement as KCommonElement
import com.strumenta.kolasu.model.Documentation as KDocumentation
import com.strumenta.kolasu.model.EntityDeclaration as KEntityDeclaration
import com.strumenta.kolasu.model.EntityGroupDeclaration as KEntityGroupDeclaration
import com.strumenta.kolasu.model.Expression as KExpression
import com.strumenta.kolasu.model.Parameter as KParameter
import com.strumenta.kolasu.model.PlaceholderElement as KPlaceholderElement
import com.strumenta.kolasu.model.Statement as KStatement
import com.strumenta.kolasu.model.TypeAnnotation as KTypeAnnotation
import com.strumenta.kolasu.parsing.ParsingResult as KParsingResult
import com.strumenta.kolasu.validation.Issue as KIssue

private const val PLACEHOLDER_NODE = "PlaceholderNode"

val LIONWEB_VERSION_USED_BY_KOLASU = LionWebVersion.v2023_1

/**
 * When this object is referenced the initialization is performed. When that happens the serializers and deserializers
 * for Position and other primitive types are registered in the MetamodelRegistry.
 */
@Deprecated("Use the AST language from Starlasu-specs")
object StarLasuLWLanguage : Language(LIONWEB_VERSION_USED_BY_KOLASU, "com.strumenta.StarLasu") {
    val CommonElement: Interface
    val Issue: Concept
    val ParsingResult: Concept

    init {
        id = "com-strumenta-StarLasu"
        key = "com_strumenta_starlasu"
        version = "1"
        createPrimitiveType("Char")
        createPrimitiveType("Point")
        val position = createPrimitiveType("Position")
        val astNode =
            createConcept("ASTNode").apply {
                createProperty(Node::position.name, position, Multiplicity.OPTIONAL)
            }
        astNode.createReference("originalNode", astNode, Multiplicity.OPTIONAL)
        astNode.createReference("transpiledNodes", astNode, Multiplicity.ZERO_TO_MANY)

        addPlaceholderNodeAnnotation(astNode)

        CommonElement = createInterface(KCommonElement::class.simpleName!!)
        createInterface(KBehaviorDeclaration::class.simpleName!!).apply { addExtendedInterface(CommonElement) }
        createInterface(KDocumentation::class.simpleName!!).apply { addExtendedInterface(CommonElement) }
        createInterface(KEntityDeclaration::class.simpleName!!).apply { addExtendedInterface(CommonElement) }
        createInterface(KEntityGroupDeclaration::class.simpleName!!).apply { addExtendedInterface(CommonElement) }
        createInterface(KExpression::class.simpleName!!).apply { addExtendedInterface(CommonElement) }
        createInterface(KParameter::class.simpleName!!).apply { addExtendedInterface(CommonElement) }
        createInterface(KPlaceholderElement::class.simpleName!!).apply { addExtendedInterface(CommonElement) }
        createInterface(KStatement::class.simpleName!!).apply { addExtendedInterface(CommonElement) }
        createInterface(KTypeAnnotation::class.simpleName!!).apply { addExtendedInterface(CommonElement) }

        Issue =
            createConcept(KIssue::class.simpleName!!).apply {
                createProperty(KIssue::type.name, addEnumerationFromClass(this@StarLasuLWLanguage, IssueType::class))
                createProperty(KIssue::message.name, LionCoreBuiltins.getString(lionWebVersion))
                createProperty(
                    KIssue::severity.name,
                    addEnumerationFromClass(this@StarLasuLWLanguage, IssueSeverity::class),
                )
                createProperty(KIssue::position.name, position, Multiplicity.OPTIONAL)
            }

        createPrimitiveType(TokensList::class)
        ParsingResult =
            createConcept(KParsingResult::class.simpleName!!).apply {
                createContainment(KParsingResult<*>::issues.name, ASTLanguage.getIssue(), Multiplicity.ZERO_TO_MANY)
                createContainment(KParsingResult<*>::root.name, ASTNode, Multiplicity.OPTIONAL)
                createProperty(
                    KParsingResult<*>::code.name,
                    LionCoreBuiltins.getString(lionWebVersion),
                    Multiplicity.OPTIONAL,
                )
                createProperty(
                    ParsingResultWithTokens<*>::tokens.name,
                    MetamodelRegistry.getPrimitiveType(TokensList::class, LIONWEB_VERSION_USED_BY_KOLASU)!!,
                    Multiplicity.OPTIONAL,
                )
            }
        MetamodelRegistry.registerMapping(IssueNode::class, ASTLanguage.getIssue())
        MetamodelRegistry.registerMapping(ParsingResultNode::class, ParsingResult)
        registerSerializersAndDeserializersInMetamodelRegistry()
    }

    private fun addPlaceholderNodeAnnotation(astNode: Concept) {
        val placeholderNodeAnnotation =
            Annotation(
                this,
                PLACEHOLDER_NODE,
                idForContainedElement(PLACEHOLDER_NODE),
                keyForContainedElement(PLACEHOLDER_NODE),
            )
        placeholderNodeAnnotation.annotates = LionCore.getConcept(lionWebVersion)

        val placeholderNodeAnnotationType =
            Enumeration(
                this,
                "PlaceholderNodeType",
            ).apply {
                this.id = "${placeholderNodeAnnotation.id!!.removeSuffix("-id")}-$name-id"
                this.key = "${placeholderNodeAnnotation.key!!.removeSuffix("-key")}-$name-key"
                addLiteral("MissingASTTransformation")
                addLiteral("FailingASTTransformation")
            }
        addElement(placeholderNodeAnnotationType)

        val reference =
            Reference(lionWebVersion).apply {
                this.name = "originalNode"
                this.id = "${placeholderNodeAnnotation.id!!.removeSuffix("-id")}-$name-id"
                this.key = "${placeholderNodeAnnotation.key!!.removeSuffix("-key")}-$name-key"
                this.type = astNode
                this.setOptional(true)
                this.setMultiple(false)
            }
        placeholderNodeAnnotation.addFeature(reference)
        val type =
            Property(lionWebVersion).apply {
                this.name = "type"
                this.id = "${placeholderNodeAnnotation.id!!.removeSuffix("-id")}-$name-id"
                this.key = "${placeholderNodeAnnotation.key!!.removeSuffix("-key")}-$name-key"
                this.type = placeholderNodeAnnotationType
                this.setOptional(false)
            }
        placeholderNodeAnnotation.addFeature(type)
        val message =
            Property(lionWebVersion).apply {
                this.name = "message"
                this.id = "${placeholderNodeAnnotation.id!!.removeSuffix("-id")}-$name-id"
                this.key = "${placeholderNodeAnnotation.key!!.removeSuffix("-key")}-$name-key"
                this.type = LionCoreBuiltins.getString(LIONWEB_VERSION_USED_BY_KOLASU)
                this.setOptional(false)
            }
        placeholderNodeAnnotation.addFeature(message)
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

    val PlaceholderNodeTypeProperty: Property
        get() = PlaceholderNode.getPropertyByName("type")!!

    val PlaceholderNodeMessageProperty: Property
        get() = PlaceholderNode.getPropertyByName("message")!!

    val PlaceholderNodeType: Enumeration
        get() = StarLasuLWLanguage.getEnumerationByName("PlaceholderNodeType")!!

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
    val charDeserializer =
        PrimitiveDeserializer<Char> { serialized ->
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

    val positionSerializer =
        PrimitiveSerializer<Position> { value ->
            "${pointSerializer.serialize((value as Position).start)}-${pointSerializer.serialize(value.end)}"
        }
    val positionDeserializer =
        PrimitiveDeserializer<Position> { serialized ->
            if (serialized == null) {
                null
            } else {
                val parts = serialized.split("-")
                require(parts.size == 2) {
                    "Position has an expected format: $serialized"
                }
                Position(pointDeserializer.deserialize(parts[0]), pointDeserializer.deserialize(parts[1]))
            }
        }
    MetamodelRegistry.addSerializerAndDeserializer(
        StarLasuLWLanguage.Position,
        positionSerializer,
        positionDeserializer,
    )

    val tokensListPrimitiveSerializer =
        PrimitiveSerializer<TokensList?> { value: TokensList? ->
            value?.tokens?.joinToString(";") { kt ->
                kt.category.type + "$" + positionSerializer.serialize(kt.position)
            }
        }
    val tokensListPrimitiveDeserializer =
        PrimitiveDeserializer<TokensList?> { serialized ->
            if (serialized == null) {
                null
            } else {
                val tokens =
                    if (serialized.isEmpty()) {
                        mutableListOf()
                    } else {
                        serialized.split(";").map {
                            val parts = it.split("$")
                            require(parts.size == 2)
                            val category = parts[0]
                            val position = positionDeserializer.deserialize(parts[1])
                            KolasuToken(TokenCategory(category), position)
                        }.toMutableList()
                    }
                TokensList(tokens)
            }
        }
    val tlpt =
        MetamodelRegistry.getPrimitiveType(TokensList::class, LIONWEB_VERSION_USED_BY_KOLASU)
            ?: throw IllegalStateException("Unknown primitive type class ${TokensList::class}")
    MetamodelRegistry.addSerializerAndDeserializer(tlpt, tokensListPrimitiveSerializer, tokensListPrimitiveDeserializer)
}

class TokensList(val tokens: List<KolasuToken>)
