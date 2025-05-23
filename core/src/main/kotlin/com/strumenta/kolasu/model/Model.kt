package com.strumenta.kolasu.model

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
