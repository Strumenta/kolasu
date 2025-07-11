package com.strumenta.kolasu.model

import io.lionweb.lioncore.java.model.AnnotationInstance
import java.io.Serializable
import java.util.concurrent.CopyOnWriteArrayList
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

class SimpleOrigin(
    override val position: Position?,
    override val sourceText: String? = null,
) : Origin, Serializable

data class CompositeOrigin(
    val elements: List<Origin>,
    override val position: Position?,
    override val sourceText: String?,
) : Origin, Serializable

interface Destination

data class CompositeDestination(val elements: List<Destination>) : Destination, Serializable {
    constructor(vararg elements: Destination) : this(elements.toList())
}

data class TextFileDestination(val position: Position?) : Destination, Serializable

val RESERVED_FEATURE_NAMES = setOf("parent", "position")

fun <N : BaseASTNode> N.withPosition(position: Position?): N {
    this.position = position
    return this
}


/**
 * The Abstract Syntax Tree will be constituted by instances of Node.
 *
 * It implements Origin as it could be the source of a AST-to-AST transformation, so the node itself can be
 * the Origin of another node.
 */
open class Node() : Origin, Destination, Serializable, HasID {

    @Internal
    override var id: String? = null

    @Internal
    val annotations: MutableList<AnnotationInstance> = CopyOnWriteArrayList()

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

    fun setSourceForTree(source: Source): Node {
        this.source = source
        this.walk().forEach {
            it.source = source
        }
        return this
    }

fun <N : BaseASTNode> N.withOrigin(origin: Origin?): N {
    this.origin =
        if (origin == this) {
            null
        } else {
            origin
        }
    return this
}

val <T : Any> Class<T>.nodeProperties: Collection<KProperty1<T, *>>
    get() = this.kotlin.nodeProperties
val <T : Any> Class<T>.nodeOriginalProperties: Collection<KProperty1<T, *>>
    get() = this.kotlin.nodeOriginalProperties
val <T : Any> KClass<T>.nodeProperties: Collection<KProperty1<T, *>>
    get() =
        memberProperties.asSequence()
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
    get() =
        nodeProperties
            .filter { it.findAnnotation<Derived>() == null }

/**
 * @return all properties of this node that are considered AST properties.
 */
val <T : BaseASTNode> T.nodeProperties: Collection<KProperty1<T, *>>
    get() = this.javaClass.nodeProperties

/**
 * @return all non-derived properties of this node that are considered AST properties.
 */
val <T : BaseASTNode> T.nodeOriginalProperties: Collection<KProperty1<T, *>>
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
