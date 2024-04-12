package com.strumenta.kolasu.model

import com.strumenta.kolasu.language.Attribute
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.Reference
import com.strumenta.kolasu.traversing.walk
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.cast
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

interface Origin {
    val position: Position?
    val sourceText: String?
    val source: Source?
        get() = position?.source
}

class SimpleOrigin(
    override val position: Position?,
    override val sourceText: String? = null
) : Origin, Serializable

data class CompositeOrigin(
    val elements: List<Origin>,
    override val position: Position?,
    override val sourceText: String?
) : Origin, Serializable

interface Destination

data class CompositeDestination(val elements: List<Destination>) : Destination, Serializable
data class TextFileDestination(val position: Position?) : Destination, Serializable

val RESERVED_FEATURE_NAMES = setOf("parent", "position")

/**
 * The Abstract Syntax Tree will be constituted by instances of Node.
 *
 * It implements Origin as it could be the source of a AST-to-AST transformation, so the node itself can be
 * the Origin of another node.
 */
open class Node() : Origin, Destination, Serializable {

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
        get() = try {
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
        get() = try {
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
    var parent: Node? = null

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
        get() = explicitlySetSource ?: origin?.source
        set(value) {
            // This is a limit of the current API: to specify a Source we need to specify coordinates
            if (this.position == null) {
                explicitlySetSource = value
            } else {
                this.origin = SimpleOrigin(this.position!!.copy(source = value))
            }
            require(this.source === value)
        }

    fun setSourceForTree(source: Source): Node {
        this.source = source
        this.walk().forEach {
            it.source = source
        }
        return this
    }

    fun detach(keepPosition: Boolean = true, keepSourceText: Boolean = false) {
        val existingOrigin = origin
        if (existingOrigin != null) {
            if (keepPosition || keepSourceText) {
                this.origin = SimpleOrigin(
                    if (keepPosition) existingOrigin.position else null,
                    if (keepSourceText) existingOrigin.sourceText else null
                )
            } else {
                this.origin = null
            }
            if (existingOrigin is Node && existingOrigin.destination == this) {
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
        return "${this.nodeType}(${properties.joinToString(", ") { "${it.name}=${it.valueToString()}" }})"
    }

    fun getChildren(containment: Containment, includeDerived: Boolean = false): List<Node> {
        return getChildren(containment.name, includeDerived)
    }

    fun getChildren(propertyName: String, includeDerived: Boolean = false): List<Node> {
        checkFeatureName(propertyName)
        val property = (if (includeDerived) properties else originalProperties)
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
}

fun <N : Node> N.withPosition(position: Position?): N {
    this.position = position
    return this
}

fun <N : Node> N.withOrigin(origin: Origin?): N {
    this.origin = if (origin == this) { null } else { origin }
    return this
}

val <T : Any> Class<T>.nodeProperties: Collection<KProperty1<T, *>>
    get() = this.kotlin.nodeProperties
val <T : Any> Class<T>.nodeOriginalProperties: Collection<KProperty1<T, *>>
    get() = this.kotlin.nodeOriginalProperties
val <T : Any> KClass<T>.nodeProperties: Collection<KProperty1<T, *>>
    get() = memberProperties.asSequence()
        .filter { it.visibility == KVisibility.PUBLIC }
        .filter { it.findAnnotation<Internal>() == null }
        .filter { it.findAnnotation<Link>() == null }
        .map {
            require(it.name !in RESERVED_FEATURE_NAMES) {
                "Property ${it.name} in ${this.qualifiedName} should be marked as internal"
            }
            it
        }
        .toList()

val <T : Any> KClass<T>.nodeOriginalProperties: Collection<KProperty1<T, *>>
    get() = nodeProperties
        .filter { it.findAnnotation<Derived>() == null }

/**
 * @return all properties of this node that are considered AST properties.
 */
val <T : Node> T.nodeProperties: Collection<KProperty1<T, *>>
    get() = this.javaClass.nodeProperties

/**
 * @return all non-derived properties of this node that are considered AST properties.
 */
val <T : Node> T.nodeOriginalProperties: Collection<KProperty1<T, *>>
    get() = this.javaClass.nodeOriginalProperties

/**
 * Use this to mark properties that are internal, i.e., they are used for bookkeeping and are not part of the model,
 * so that they will not be considered branches of the AST.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Internal

/**
 * Use this to mark all relations which are secondary, i.e., they are calculated from other relations,
 * so that they will not be considered branches of the AST.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Derived

/**
 * Use this to mark all the properties that return a Node or a list of Nodes which are not
 * contained by the Node having the properties. In other words: they are just references.
 * This will prevent them from being considered branches of the AST.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Link

/**
 * Use this to mark something that does not inherit from Node as a node, so it will be included in the AST.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class NodeType

fun checkFeatureName(featureName: String) {
    require(featureName !in RESERVED_FEATURE_NAMES) { "$featureName is not a valid feature name" }
}

/**
 * Use this to mark a type representing an AST Root.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ASTRoot(val canBeNotRoot: Boolean = false)
