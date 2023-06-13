package com.strumenta.kolasu.symbolresolution

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.nodeProperties
import com.strumenta.kolasu.traversing.walk
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.test.assertTrue

fun Node.assertAllReferencesResolved(
    withReturnType: KClass<out PossiblyNamed> = PossiblyNamed::class
) = assertTrue {
    this.getReferenceResolvedValues(withReturnType).all { it }
}

fun Node.assertNotAllReferencesResolved(
    withReturnType: KClass<out PossiblyNamed> = PossiblyNamed::class
) = assertTrue {
    this.getReferenceResolvedValues(withReturnType).ifEmpty { sequenceOf(false) }.any { !it }
}

fun Node.assertAllReferencesResolved(
    forProperty: ReferenceByNameProperty
) = assertTrue {
    this.getReferenceResolvedValues(forProperty).all { it }
}

fun Node.assertNotAllReferencesResolved(
    forProperty: ReferenceByNameProperty
) = assertTrue {
    this.getReferenceResolvedValues(forProperty).ifEmpty { sequenceOf(false) }.any { !it }
}

private fun Node.getReferenceResolvedValues(
    withReturnType: KClass<out PossiblyNamed> = PossiblyNamed::class
): Sequence<Boolean> {
    return this.walk().flatMap {
        it.nodeProperties
            .filter { property -> property.returnType.isSubtypeOf(referenceByName(withReturnType)) }
            .mapNotNull { property -> property.get(it) }
            .map { value -> (value as ReferenceByName<*>).isResolved }
    }
}

private fun Node.getReferenceResolvedValues(
    forProperty: ReferenceByNameProperty
): Sequence<Boolean> {
    return this.walk().flatMap {
        it.nodeProperties
            .filter { property -> property == forProperty }
            .mapNotNull { property -> property.get(it) }
            .map { value -> (value as ReferenceByName<*>).isResolved }
    }
}

private fun referenceByName(targetClass: KClass<out PossiblyNamed>): KType {
    return ReferenceByName::class.createType(
        arguments = listOf(KTypeProjection(variance = KVariance.OUT, type = targetClass.createType()))
    )
}
