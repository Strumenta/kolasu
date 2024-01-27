package com.strumenta.kolasu.model

import com.strumenta.kolasu.ast.Derived
import com.strumenta.kolasu.ast.Internal
import com.strumenta.kolasu.ast.Link
import com.strumenta.kolasu.ast.Origin
import com.strumenta.kolasu.ast.Range
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
val <T : Any> KClass<T>.nodeProperties: Collection<KProperty1<T, *>>
    get() =
        memberProperties
            .asSequence()
            .filter { it.visibility == KVisibility.PUBLIC }
            .filter { it.findAnnotation<Derived>() == null }
            .filter { it.findAnnotation<Internal>() == null }
            .filter { it.findAnnotation<Link>() == null }
            .toList()

/**
 * @return all properties of this node that are considered AST properties.
 */
val <T : NodeLike> T.nodeProperties: Collection<KProperty1<T, *>>
    get() = this.javaClass.nodeProperties
