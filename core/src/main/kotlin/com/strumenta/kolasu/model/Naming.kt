package com.strumenta.kolasu.model

import com.strumenta.kolasu.ast.NodeLike
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf

/**
 * Try to resolve the reference by finding a named element with a matching name.
 * The name match is performed in a case-sensitive or insensitive way depending on the value of @param[caseInsensitive].
 */
fun <N> ReferenceByName<N>.tryToResolve(
    candidates: Iterable<N>,
    caseInsensitive: Boolean = false,
): Boolean where N : PossiblyNamed {
    val res: N? = candidates.find { if (it.name == null) false else it.name.equals(this.name, caseInsensitive) }
    this.referred = res
    return res != null
}

/**
 * Try to resolve the reference by assigning [possibleValue]. The assignment is not performed if
 * [possibleValue] is null.
 *
 * @param possibleValue the candidate value.
 * @return true if the assignment has been performed
 */
fun <N> ReferenceByName<N>.tryToResolve(possibleValue: N?): Boolean where N : PossiblyNamed =
    if (possibleValue == null) {
        false
    } else {
        this.referred = possibleValue
        true
    }

/**
 * Typealias representing reference properties.
 **/
typealias KReferenceByName<S> = KProperty1<S, ReferenceByName<out PossiblyNamed>?>

/**
 * Builds a type representation for a reference
 **/
fun kReferenceByNameType(targetClass: KClass<out PossiblyNamed> = PossiblyNamed::class): KType =
    ReferenceByName::class.createType(
        arguments = listOf(KTypeProjection(variance = KVariance.OUT, type = targetClass.createType())),
        nullable = true,
    )

/**
 * Retrieves the referred type for a given reference property.
 **/
@Suppress("unchecked_cast")
fun KReferenceByName<*>.getReferredType(): KClass<out PossiblyNamed> =
    this
        .returnType
        .arguments[0]
        .type!!
        .classifier!! as KClass<out PossiblyNamed>

/**
 * Retrieves all reference properties for a given node.
 **/
fun NodeLike.kReferenceByNameProperties(targetClass: KClass<out PossiblyNamed> = PossiblyNamed::class) =
    this.nodeProperties.filter { it.returnType.isSubtypeOf(kReferenceByNameType(targetClass)) }
