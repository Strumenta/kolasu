package com.strumenta.kolasu.language

import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.asAttribute
import com.strumenta.kolasu.model.containedType
import com.strumenta.kolasu.model.isAttribute
import com.strumenta.kolasu.model.isContainment
import com.strumenta.kolasu.model.isMarkedAsNodeType
import com.strumenta.kolasu.model.isReference
import com.strumenta.kolasu.model.nodeProperties
import com.strumenta.kolasu.model.referredType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.superclasses

/**
 * There is no explicit Language defined in Kolasu, it is just a bunch of AST classes.
 * We create this Class to represent that collection of AST classes.
 */
class KolasuLanguage(val qualifiedName: String) {
    val astClasses: List<KClass<*>>
        get() = _astClasses
    val enumClasses: List<KClass<out Enum<*>>>
        get() = _enumClasses
    private val _astClasses: MutableList<KClass<*>> = mutableListOf()
    private val _enumClasses: MutableList<KClass<out Enum<*>>> = mutableListOf()

    val simpleName: String
        get() = qualifiedName.split(".").last()

    fun addEnumClass(kClass: KClass<out Enum<*>>) {
        _enumClasses.add(kClass)
    }

    fun addInterfaceClass(kClass: KClass<*>): Boolean {
        if (!_astClasses.contains(kClass) && _astClasses.add(kClass)) {
            kClass.supertypes.forEach { superType -> processSuperType(superType) }
            return true
        } else {
            return false
        }
    }

    private fun processSuperType(superType: KType) {
        val kClass = superType.classifier as? KClass<*>
        when (kClass) {
            null -> Unit
            Node::class -> Unit
            Named::class -> Unit
            Any::class -> Unit
            else -> {
                if (kClass.java.isInterface) {
                    if (kClass.isMarkedAsNodeType()) {
                        addInterfaceClass(kClass)
                    }
                } else {
                    if (kClass.isSubclassOf(Node::class)) {
                        addClass(kClass as KClass<out Node>)
                    }
                }
            }
        }
    }

    fun <N : Node> addClass(kClass: KClass<N>): Boolean {
        if (!_astClasses.contains(kClass) && _astClasses.add(kClass)) {
            kClass.supertypes.forEach { superType ->
                processSuperType(superType)
            }
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
                } else if (nodeProperty.isAttribute()) {
                    val kClass = nodeProperty.asAttribute().type.classifier as? KClass<*>
                    if (kClass != null && kClass.superclasses.contains(Enum::class)) {
                        addEnumClass(kClass as KClass<out Enum<*>>)
                    }
                }
            }
            return true
        } else {
            return false
        }
    }

    fun findASTClass(name: String): KClass<*>? = astClasses.find { it.simpleName == name }
    fun findEnumClass(name: String): KClass<out Enum<*>>? = enumClasses.find { it.simpleName == name }
}
