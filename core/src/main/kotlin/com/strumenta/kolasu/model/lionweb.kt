package com.strumenta.kolasu.model

import com.strumenta.kolasu.metamodel.StarLasuMetamodel
import org.lionweb.lioncore.java.metamodel.Concept
import org.lionweb.lioncore.java.metamodel.Containment
import org.lionweb.lioncore.java.metamodel.LionCoreBuiltins
import org.lionweb.lioncore.java.metamodel.Metamodel
import org.lionweb.lioncore.java.metamodel.Property
import org.lionweb.lioncore.java.metamodel.Reference
import java.lang.IllegalStateException
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmName

private val conceptsMemory = HashMap<KClass<out ASTNode>, Concept>()

val <A : ASTNode>KClass<A>.concept: Concept
    get() = conceptsMemory.getOrElse(this) { calculateConcept(this, conceptsMemory) }


private fun metamodelFor(kClass: KClass<out ASTNode>) : Metamodel? {
    val metamodelInstance : Metamodel? = if (kClass.jvmName.contains("$")) {
        val outerClass = kClass.java.declaringClass.kotlin
        val metamodelKClass = outerClass.nestedClasses.find { it.simpleName == "Metamodel" }
        if (metamodelKClass == null) {
            return metamodelFor(outerClass as KClass<out ASTNode>)
        }
        metamodelKClass?.objectInstance as? Metamodel
    } else {
        val metamodelQName = kClass.qualifiedName!!.removeSuffix(".${kClass.simpleName}") + ".Metamodel"
        val classLoader = kClass.java.classLoader ?: throw IllegalStateException("No class loader for ${kClass.java}")
        val metamodelKClass = try {
            classLoader.loadClass(metamodelQName).kotlin
        } catch (e: ClassNotFoundException) {
            throw RuntimeException("Unable to find the metamodel for Kotlin class $kClass")
        }
        metamodelKClass.staticProperties.find { it.name == "INSTANCE"}?.let { instance ->
            instance.get() as Metamodel
        } ?: metamodelKClass.objectInstance as Metamodel
    }
    return metamodelInstance
}

fun <A : ASTNode> calculateConcept(kClass: KClass<A>, conceptsMemory: HashMap<KClass<out ASTNode>, Concept>): Concept {
    try {
        if (kClass == ASTNode::class) {
            return StarLasuMetamodel.astNode
        }
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

        val metamodelInstance : Metamodel = metamodelFor(kClass) ?: throw RuntimeException("No Metamodel object for ${kClass}")

        // We need to add it right away because of nodes referring to themselves
        conceptsMemory[kClass] = concept

        kClass.nodeProperties.forEach {
            val provideNodes = PropertyDescription.providesNodes(it as KProperty1<in ASTNode, *>)
            val ref = (it as KProperty1<in ASTNode, *>).isReference
            when {
                !provideNodes -> {
                    if (it.returnType.classifier == ReferenceByName::class) {
                        val reference = Reference()
                        reference.simpleName = it.name
                        reference.id = it.name
                        reference.type = (
                                it.returnType.arguments[0].type!!
                                    .classifier as KClass<out ASTNode>
                                ).concept
                        concept.addFeature(reference)
                    } else {
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

                            Position::class -> {
                                property.type = StarLasuMetamodel.position
                            }

                            else -> {
                                if ((it.returnType.classifier as? KClass<*>)?.allSuperclasses?.contains(Enum::class)
                                        ?: false
                                ) {
                                    // TODO add support for enums
                                    property.type = LionCoreBuiltins.getString()
                                } else {
                                    TODO("Return type: ${it.returnType.classifier} (${it.returnType.classifier?.javaClass} for property ${it}")
                                }
                            }
                        }
                        concept.addFeature(property)
                    }
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
