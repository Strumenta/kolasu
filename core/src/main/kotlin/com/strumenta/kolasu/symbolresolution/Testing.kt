package com.strumenta.kolasu.symbolresolution

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.nodeProperties
import com.strumenta.kolasu.traversing.walk
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.test.assertTrue

fun Node.assertAllReferencesResolved() {
    this.assertAllReferencesOfTypeResolved<PossiblyNamed>()
}

fun Node.assertNotAllReferencesResolved() {
    this.assertNotAllReferencesOfTypeResolved<PossiblyNamed>()
}

inline fun <reified T : PossiblyNamed> Node.assertAllReferencesOfTypeResolved() {
    assertTrue { this.getReferenceResolutionInfo<T>().all { it } }
}

inline fun <reified T : PossiblyNamed> Node.assertNotAllReferencesOfTypeResolved() {
    assertTrue { this.getReferenceResolutionInfo<T>().ifEmpty { sequenceOf(false) }.any { !it } }
}

fun Node.assertAllReferencesOfPropertyResolved(targetProperty: ReferenceByNameProperty) {
    assertTrue { this.getReferenceResolutionInfo(targetProperty).all { it } }
}

fun Node.assertNotAllReferencesOfPropertyResolved(targetProperty: ReferenceByNameProperty) {
    assertTrue { this.getReferenceResolutionInfo(targetProperty).ifEmpty { sequenceOf(false) }.any { !it } }
}

inline fun <reified T : PossiblyNamed> Node.getReferenceResolutionInfo(): Sequence<Boolean> {
    return this.walk().flatMap {
        it.nodeProperties
            .filter { property -> property.returnType.isSubtypeOf(referenceByNameType<T>()) }
            .mapNotNull { property -> property.get(it) }
            .map { value -> (value as ReferenceByName<*>).resolved }
    }
}

fun Node.getReferenceResolutionInfo(targetProperty: ReferenceByNameProperty): Sequence<Boolean> {
    return this.walk().flatMap {
        it.nodeProperties
            .filter { property -> property == targetProperty }
            .mapNotNull { property -> property.get(it) }
            .map { value -> (value as ReferenceByName<*>).resolved }
    }
}

inline fun <reified T : PossiblyNamed> referenceByNameType(): KType {
    return ReferenceByName::class.createType(
        arguments = listOf(KTypeProjection(variance = KVariance.OUT, type = T::class.createType())),
    )
}
