package com.strumenta.kolasu.model

import com.strumenta.kolasu.metamodel.StarLasuMetamodel
import org.lionweb.lioncore.java.metamodel.Concept
import org.lionweb.lioncore.java.metamodel.Containment
import org.lionweb.lioncore.java.metamodel.LionCoreBuiltins
import org.lionweb.lioncore.java.metamodel.Metamodel
import org.lionweb.lioncore.java.metamodel.Property
import java.lang.IllegalStateException
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.superclasses

private val conceptsMemory = HashMap<KClass<out ASTNode>, Concept>()

val <A : ASTNode>KClass<A>.concept: Concept
    get() = conceptsMemory.getOrPut(this) { calculateConcept(this) }

fun <A : ASTNode> calculateConcept(kClass: KClass<A>): Concept {
    try {
        require(kClass.allSuperclasses.contains(ASTNode::class)) {
            "KClass $kClass is not a subclass of ASTNode"
        }
        val concept = Concept()
        concept.simpleName = kClass.simpleName
        concept.id = kClass.simpleName

        val superclasses = kClass.superclasses.toMutableList()
        // require(superclasses.contains(ASTNode::class))
        superclasses.remove(ASTNode::class)
        superclasses.forEach { superclass ->
            if (superclass.java.isInterface) {
                // TODO consider
            } else {
                concept.extendedConcept = (superclass as KClass<out ASTNode>).concept
            }
        }
        if (concept.extendedConcept == null) {
            concept.extendedConcept = StarLasuMetamodel.astNode
        }

        val metamodelQName = kClass.qualifiedName!!.removeSuffix(".${kClass.simpleName}") + ".Metamodel"
        val classLoader = kClass.java.classLoader ?: throw IllegalStateException("No class loader for ${kClass.java}")
        val metamodelKClass = classLoader.loadClass(metamodelQName).kotlin
        val metamodelInstance = metamodelKClass.objectInstance as Metamodel
        metamodelInstance.addElement(concept)

        kClass.nodeProperties.forEach {
            val provideNodes = PropertyDescription.providesNodes(it as KProperty1<in ASTNode, *>)
            val ref = (it as KProperty1<in ASTNode, *>).isReference
            when {
                !provideNodes -> {
                    val property = Property()
                    property.simpleName = it.name
                    property.id = it.name
                    when (it.returnType.classifier) {
                        Boolean::class -> {
                            property.type = LionCoreBuiltins.getBoolean()
                        }
                        String::class -> {
                            property.type = LionCoreBuiltins.getString()
                        }
                        Int::class -> {
                            property.type = LionCoreBuiltins.getInteger()
                        }
                        else -> {
                            if ((it.returnType.classifier as? KClass<*>)?.allSuperclasses?.contains(Enum::class)
                                ?: false
                            ) {
                                // TODO add support for enums
                                property.type = LionCoreBuiltins.getString()
                            } else {
                                TODO("Return type: ${it.returnType.classifier} (${it.returnType.classifier?.javaClass}")
                            }
                        }
                    }
                    concept.addFeature(property)
                }
                provideNodes && ref -> TODO()
                provideNodes && !ref -> {
                    val containment = Containment()
                    containment.simpleName = it.name
                    containment.id = it.name
                    if ((it.returnType.classifier as KClass<*>).allSupertypes.map { it.classifier }
                        .contains(Collection::class)
                    ) {
                        containment.isMultiple = true
                        containment.type = (
                            it.returnType.arguments[0].type!!
                                .classifier as KClass<out ASTNode>
                            ).concept
                    } else {
                        containment.type = (it.returnType.classifier as KClass<out ASTNode>).concept
                    }
                    concept.addFeature(containment)
                }
            }
        }
        return concept
    } catch (e: Throwable) {
        throw RuntimeException("Unable to calculate concept for $kClass", e)
    }
}
