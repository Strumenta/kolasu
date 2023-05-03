package com.strumenta.kolasu.symbolresolution

import com.strumenta.kolasu.model.ASTNode
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.nodeProperties
import com.strumenta.kolasu.traversing.walk
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.test.assertTrue

fun ASTNode.assertAllReferencesResolved() {
    this.walk().forEach {
        it.nodeProperties
            .filter { property ->
                property.returnType.isSubtypeOf(
                    ReferenceByName::class.createType(
                        arguments = listOf(
                            KTypeProjection(variance = KVariance.OUT, type = PossiblyNamed::class.createType())
                        )
                    )
                )
            }
            .forEach { property -> assertTrue { (property.get(it) as ReferenceByName<*>).resolved } }
    }
}
