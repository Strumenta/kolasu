package com.strumenta.kolasu.model

import com.strumenta.kolasu.metamodel.StarLasuMetamodel
import org.lionweb.lioncore.java.metamodel.Concept
import org.lionweb.lioncore.java.metamodel.Containment
import org.lionweb.lioncore.java.metamodel.LionCoreBuiltins
import org.lionweb.lioncore.java.metamodel.Metamodel
import org.lionweb.lioncore.java.metamodel.Property
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.superclasses

private val conceptsMemory = HashMap<KClass<out ASTNode>, Concept>()

val <A:ASTNode>KClass<A>.concept : Concept
    get() = conceptsMemory.getOrPut(this) { calculateConcept(this) }

fun <A:ASTNode>calculateConcept(kClass: KClass<A>) : Concept {
    val concept = Concept()
    concept.simpleName = kClass.simpleName

    val superclasses = kClass.superclasses.toMutableList()
    require(superclasses.contains(ASTNode::class))
    superclasses.remove(ASTNode::class)
    if (superclasses.isEmpty()) {
        concept.extendedConcept = StarLasuMetamodel.astNode
    } else {
        TODO()
    }

    val metamodelQName = kClass.qualifiedName!!.removeSuffix(".${kClass.simpleName}") + ".Metamodel"
    val metamodelKClass = kClass.java.classLoader.loadClass(metamodelQName).kotlin
    val metamodelInstance = metamodelKClass.objectInstance as Metamodel
    metamodelInstance.addElement(concept)

    kClass.nodeProperties.forEach {
        val provideNodes = PropertyDescription.providesNodes(it as KProperty1<in ASTNode, *>)
        val ref = (it as KProperty1<in ASTNode, *>).isReference
        when {
            !provideNodes -> {
                val property = Property()
                property.simpleName = it.name
                when (it.returnType) {
                    Boolean::class.createType() -> {
                        property.type = LionCoreBuiltins.getBoolean()
                    }
                    String::class.createType() -> {
                        property.type = LionCoreBuiltins.getString()
                    }
                    Int::class.createType() -> {
                        property.type = LionCoreBuiltins.getInteger()
                    }
                    else -> {
                        TODO()
                    }
                }
                concept.addFeature(property)
            }
            provideNodes && ref -> TODO()
            provideNodes && !ref -> {
                val containment = Containment()
                containment.simpleName = it.name
                if ((it.returnType.classifier as KClass<*>) == List::class) {
                    containment.isMultiple = true
                    containment.type = (it.returnType.arguments[0].type!!.classifier as KClass<out ASTNode>).concept
                } else {
                    containment.type = (it.returnType.classifier as KClass<out ASTNode>).concept
                }
                concept.addFeature(containment)
            }
        }
    }
    return concept
}
