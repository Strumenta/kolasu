package com.strumenta.kolasu.model

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

fun <N : NodeLike> N.withRange(range: Range?): N {
    this.range = range
    return this
}

fun <N : NodeLike> N.withOrigin(origin: Origin?): N {
    this.origin =
        if (origin == NodeOrigin(this)) {
            null
        } else {
            origin
        }
    return this
}

fun <N : NodeLike> N.withOrigin(node: NodeLike): N {
    this.origin =
        if (node == this) {
            null
        } else {
            NodeOrigin(node)
        }
    return this
}

val <T : Any> Class<T>.nodeProperties: Collection<KProperty1<T, *>>
    get() = this.kotlin.nodeProperties
val <T : Any> Class<T>.nodeOriginalProperties: Collection<KProperty1<T, *>>
    get() = this.kotlin.nodeOriginalProperties
val <T : Any> KClass<T>.nodeProperties: Collection<KProperty1<T, *>>
    get() =
        memberProperties
            .asSequence()
            .filter { it.visibility == KVisibility.PUBLIC }
            .filter { it.findAnnotation<Internal>() == null }
            .filter { it.findAnnotation<Link>() == null }
            .map {
                require(it.name !in RESERVED_FEATURE_NAMES) {
                    "Property ${it.name} in ${this.qualifiedName} should be marked as internal"
                }
                it
            }.toList()

val <T : Any> KClass<T>.nodeOriginalProperties: Collection<KProperty1<T, *>>
    get() =
        nodeProperties
            .filter { it.findAnnotation<Derived>() == null }

/**
 * @return all properties of this node that are considered AST properties.
 */
val <T : NodeLike> T.nodeProperties: Collection<KProperty1<T, *>>
    get() = this.javaClass.nodeProperties

/**
 * @return all non-derived properties of this node that are considered AST properties.
 */
val <T : Node> T.nodeOriginalProperties: Collection<KProperty1<T, *>>
    get() = this.javaClass.nodeOriginalProperties
