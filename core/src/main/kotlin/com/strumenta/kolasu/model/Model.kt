package com.strumenta.kolasu.model

import com.strumenta.kolasu.language.Attribute
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.Reference
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

interface Origin {
    val position: Position?
    val sourceText: String?
    val source: Source?
        get() = position?.source
}

class SimpleOrigin(override val position: Position?, override val sourceText: String?) : Origin, Serializable

data class CompositeOrigin(
    val elements: List<Origin>,
    override val position: Position?,
    override val sourceText: String?
) : Origin, Serializable

interface Destination

data class CompositeDestination(val elements: List<Destination>) : Destination, Serializable
data class TextFileDestination(val position: Position?) : Destination, Serializable

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

    @property:Internal
    override val source: Source?
        get() = origin?.source

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

    fun getChildren(containment: Containment): List<Node> {
        return when (val rawValue = nodeProperties.find { it.name == containment.name }!!.get(this)) {
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

    fun getReference(reference: Reference): ReferenceByName<*> {
        val rawValue = nodeProperties.find { it.name == reference.name }!!.get(this)
        return rawValue as ReferenceByName<*>
    }

    fun getAttributeValue(attribute: Attribute): Any? {
        return nodeProperties.find { it.name == attribute.name }!!.get(this)
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
val <T : Any> KClass<T>.nodeProperties: Collection<KProperty1<T, *>>
    get() = memberProperties.asSequence()
        .filter { it.visibility == KVisibility.PUBLIC }
        .filter { it.findAnnotation<Derived>() == null }
        .filter { it.findAnnotation<Internal>() == null }
        .filter { it.findAnnotation<Link>() == null }
        .toList()

/**
 * @return all properties of this node that are considered AST properties.
 */
val <T : Node> T.nodeProperties: Collection<KProperty1<T, *>>
    get() = this.javaClass.nodeProperties

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
annotation class Derived

/**
 * Use this to mark all the properties that return a Node or a list of Nodes which are not
 * contained by the Node having the properties. In other words: they are just references.
 * This will prevent them from being considered branches of the AST.
 */
annotation class Link

/**
 * Use this to mark something that does not inherit from Node as a node, so it will be included in the AST.
 */
annotation class NodeType
