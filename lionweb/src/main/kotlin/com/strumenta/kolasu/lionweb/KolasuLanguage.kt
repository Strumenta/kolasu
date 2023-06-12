package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.containedType
import com.strumenta.kolasu.model.isContainment
import com.strumenta.kolasu.model.isReference
import com.strumenta.kolasu.model.nodeProperties
import com.strumenta.kolasu.model.referredType
import kotlin.reflect.KClass

/**
 * There is no explicit Language defined in Kolasu, it is just a bunch of AST classes.
 * We create this Class to represent that collection of AST classes.
 */
class KolasuLanguage {
    val astClasses: MutableList<KClass<out Node>> = mutableListOf()

    fun <N: Node>addClass(kClass: KClass<N>) : Boolean {
        if (!astClasses.contains(kClass) && astClasses.add(kClass)) {
            if (kClass.isSealed) {
                kClass.sealedSubclasses.forEach {
                    addClass(it)
                }
            }
            kClass.nodeProperties.forEach { nodeProperty ->
                if (nodeProperty.isContainment()) {
                    addClass(nodeProperty.containedType())
                } else if (nodeProperty.isReference()) {
                    addClass(nodeProperty.referredType())
                }
                // TODO add enums and other datatypes
            }
            return true
        } else {
            return false
        }
    }
}