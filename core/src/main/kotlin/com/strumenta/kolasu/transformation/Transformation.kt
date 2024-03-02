package com.strumenta.kolasu.transformation

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor

/**
 * A child of an AST node that is automatically populated from a source tree.
 */
annotation class Mapped(
    val path: String = "",
)

/**
 * Sentinel value used to represent the information that a given property is not a child node.
 */
internal val NO_CHILD_NODE = ChildNodeTransformer<Any, Any, Any>("", { x -> x }, { _, _ -> })

internal fun <Source : Any, Target, Child> NodeTransformer<*, *>.getChildNodeTransformer(
    nodeClass: KClass<out Source>,
    parameterName: String,
): ChildNodeTransformer<Source, Target, Child>? {
    val childKey = nodeClass.qualifiedName + "#" + parameterName
    var childNodeTransformer = this.children[childKey]
    if (childNodeTransformer == null) {
        childNodeTransformer = this.children[parameterName]
    }
    return childNodeTransformer as ChildNodeTransformer<Source, Target, Child>?
}

internal sealed class ParameterValue

internal class PresentParameterValue(
    val value: Any?,
) : ParameterValue()

internal object AbsentParameterValue : ParameterValue()

inline fun <T : Any> KClass<T>.preferredConstructor(): KFunction<T> {
    val constructors = this.constructors
    return if (constructors.size != 1) {
        if (this.primaryConstructor != null) {
            this.primaryConstructor!!
        } else {
            throw RuntimeException(
                "Node Factories support only classes with exactly one constructor or a " +
                    "primary constructor. Class ${this.qualifiedName} has ${constructors.size}",
            )
        }
    } else {
        constructors.first()
    }
}

interface ParameterConverter {
    fun isApplicable(
        kParameter: KParameter,
        value: Any?,
    ): Boolean

    fun convert(
        kParameter: KParameter,
        value: Any?,
    ): Any?
}
