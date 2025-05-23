package com.strumenta.kolasu.model

import com.strumenta.kolasu.language.Attribute
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.Reference
import com.strumenta.kolasu.traversing.walk
import io.lionweb.lioncore.java.language.Annotation
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.Property
import io.lionweb.lioncore.java.model.AnnotationInstance
import io.lionweb.lioncore.java.model.Node
import io.lionweb.lioncore.java.model.ReferenceValue
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.cast

typealias Node = BaseASTNode

/**
 * The Abstract Syntax Tree will be constituted by instances of Node.
 *
 * It implements Origin as it could be the source of a AST-to-AST transformation, so the node itself can be
 * the Origin of another node.
 */
open class BaseASTNode() : Origin, Destination, Serializable, HasID, ASTNode {
    @Internal
    override var id: String? = null

    @Internal
    protected var positionOverride: Position? = null

    constructor(position: Position?) : this() {
        this.position = position
    }

    constructor(origin: Origin?) : this() {
        if (origin != null) {
            this.origin = origin
        }
    }

    @Internal
    open val nodeType: String
        get() = this::class.qualifiedName!!

    @Internal
    open val simpleNodeType: String
        get() = nodeType.split(".").last()

    /**
     * The properties of this AST nodes, including attributes, children, and references.
     */
    @property:Internal
    open val properties: List<PropertyDescription>
        get() =
            try {
                nodeProperties.map { PropertyDescription.buildFor(it, this) }
            } catch (e: Throwable) {
                throw RuntimeException("Issue while getting properties of node ${this::class.qualifiedName}", e)
            }

    /**
     * The properties of this AST nodes, including attributes, children, and references, but excluding derived
     * properties.
     */
    @property:Internal
    open val originalProperties: List<PropertyDescription>
        get() =
            try {
                properties.filter { !it.derived }
            } catch (e: Throwable) {
                throw RuntimeException("Issue while getting properties of node ${this::class.qualifiedName}", e)
            }

    /**
     * The node from which this AST Node has been generated, if any.
     */
    @property:Internal
    var origin: Origin? = null

    /**
     * The parent node, if any.
     */
    @property:Internal
    var parent: BaseASTNode? = null

    /**
     * The position of this node in the source text.
     * If a position has been provided when creating this node, it is returned.
     * Otherwise, the value of this property is the position of the origin, if any.
     */
    @property:Internal
    override var position: Position?
        get() = positionOverride ?: origin?.position
        set(position) {
            this.positionOverride = position
        }

    private var explicitlySetSource: Source? = null

    @property:Internal
    override var source: Source?
        get() = explicitlySetSource ?: (position?.source ?: origin?.source)
        set(value) {
            explicitlySetSource = value
            if (value == null) {
                require(this.source == null)
            } else {
                require(this.source === value) {
                    "The source has not been set correctly. It should be $value " +
                        "while it is ${this.source}"
                }
            }
        }

    fun setSourceForTree(source: Source): BaseASTNode {
        this.source = source
        this.walk().forEach {
            it.source = source
        }
        return this
    }

    fun detach(
        keepPosition: Boolean = true,
        keepSourceText: Boolean = false,
    ) {
        val existingOrigin = origin
        if (existingOrigin != null) {
            if (keepPosition || keepSourceText) {
                this.origin =
                    SimpleOrigin(
                        if (keepPosition) existingOrigin.position else null,
                        if (keepSourceText) existingOrigin.sourceText else null,
                    )
            } else {
                this.origin = null
            }
            if (existingOrigin is BaseASTNode && existingOrigin.destination == this) {
                existingOrigin.destination = null
            }
        }
    }

    /**
     * Tests whether the given position is contained in the interval represented by this object.
     * @param position the position
     */
    fun contains(position: Position?): Boolean {
        return this.position?.contains(position) ?: false
    }

    /**
     * Tests whether the given position overlaps the interval represented by this object.
     * @param position the position
     */
    fun overlaps(position: Position?): Boolean {
        return this.position?.overlaps(position) ?: false
    }

    /**
     * The source text for this node
     */
    @Internal
    override val sourceText: String?
        get() = origin?.sourceText

    @Internal
    var destination: Destination? = null

    /**
     * This must be final because otherwise data classes extending this will automatically generate
     * their own implementation. If Link properties are present it could lead to stack overflows in case
     * of circular graphs.
     */
    final override fun toString(): String {
        return "${this.nodeType}(${originalProperties.joinToString(", ") { "${it.name}=${it.valueToString()}" }})"
    }

    fun getChildren(
        containment: Containment,
        includeDerived: Boolean = false,
    ): List<Node> {
        return getChildren(containment.name, includeDerived)
    }

    fun getChildren(
        propertyName: String,
        includeDerived: Boolean = false,
    ): List<Node> {
        checkFeatureName(propertyName)
        val property =
            (if (includeDerived) properties else originalProperties)
                .find { it.name == propertyName }
        require(property != null) {
            "Property $propertyName not found in node of type ${this.nodeType} " +
                "(considering derived properties? $includeDerived)"
        }
        return when (
            val rawValue = property!!.value
        ) {
            null -> {
                emptyList()
            }

            is List<*> -> {
                rawValue as List<Node>
            }

            else -> {
                listOf(rawValue as Node)
            }
        }
    }

    fun getReference(reference: Reference): ReferenceByName<*>? {
        return getReference(reference.name)
    }

    fun getReference(name: String): ReferenceByName<*>? {
        val rawValue = properties.find { it.name == name }!!.value
        if (rawValue == null) {
            return null
        }
        return rawValue as ReferenceByName<*>
    }

    fun getAttributeValue(attribute: Attribute): Any? {
        val value = getAttributeValue(attribute.name)
        if (value == null) {
            if (!attribute.optional) {
                throw IllegalStateException("Mandatory attribute ${attribute.name} is null")
            }
        } else {
            val classifier = attribute.type.classifier
            if (classifier is KClass<*>) {
                return classifier.cast(value)
            }
        }
        return value
    }

    fun getAttributeValue(name: String): Any? {
        return properties.find { it.name == name }!!.value
    }

    override fun getPropertyValue(property: Property): Any? {
        TODO("Not yet implemented")
    }

    override fun setPropertyValue(
        property: Property,
        value: Any?,
    ) {
        TODO("Not yet implemented")
    }

    override fun getChildren(containment: io.lionweb.lioncore.java.language.Containment): List<Node?> {
        TODO("Not yet implemented")
    }

    override fun addChild(
        containment: io.lionweb.lioncore.java.language.Containment,
        child: Node,
    ) {
        TODO("Not yet implemented")
    }

    override fun removeChild(node: Node) {
        TODO("Not yet implemented")
    }

    override fun removeChild(
        containment: io.lionweb.lioncore.java.language.Containment,
        index: Int,
    ) {
        TODO("Not yet implemented")
    }

    override fun getReferenceValues(reference: io.lionweb.lioncore.java.language.Reference): List<ReferenceValue?> {
        TODO("Not yet implemented")
    }

    override fun addReferenceValue(
        reference: io.lionweb.lioncore.java.language.Reference,
        referredNode: ReferenceValue?,
    ) {
        TODO("Not yet implemented")
    }

    override fun removeReferenceValue(
        reference: io.lionweb.lioncore.java.language.Reference,
        referenceValue: ReferenceValue?,
    ) {
        TODO("Not yet implemented")
    }

    override fun removeReferenceValue(
        reference: io.lionweb.lioncore.java.language.Reference,
        index: Int,
    ) {
        TODO("Not yet implemented")
    }

    override fun setReferenceValues(
        reference: io.lionweb.lioncore.java.language.Reference,
        values: List<ReferenceValue?>,
    ) {
        TODO("Not yet implemented")
    }

    override fun getID(): String? {
        TODO("Not yet implemented")
    }

    override fun getParent(): Node? {
        TODO("Not yet implemented")
    }

    override fun getClassifier(): Concept? {
        TODO("Not yet implemented")
    }

    override fun getContainmentFeature(): io.lionweb.lioncore.java.language.Containment? {
        TODO("Not yet implemented")
    }

    override fun getAnnotations(): List<AnnotationInstance?> {
        TODO("Not yet implemented")
    }

    override fun getAnnotations(annotation: Annotation): List<AnnotationInstance?> {
        TODO("Not yet implemented")
    }

    override fun addAnnotation(instance: AnnotationInstance) {
        TODO("Not yet implemented")
    }

    override fun removeAnnotation(instance: AnnotationInstance) {
        TODO("Not yet implemented")
    }
}
