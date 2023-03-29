package com.strumenta.kolasu.model.lionweb

import com.strumenta.kolasu.metamodel.StarLasuMetamodel
import com.strumenta.kolasu.model.ASTNode
import com.strumenta.kolasu.model.commonelements.Metamodel
import org.lionweb.lioncore.java.metamodel.Concept
import org.lionweb.lioncore.java.metamodel.ConceptInterface
import org.lionweb.lioncore.java.metamodel.Enumeration
import kotlin.reflect.KClass

// If we want a class not to be part of the "natural" Metamodel (i.e., the one represented by an object in the same
// package), then we should ensure that the corresponding entry in conceptsMemory, conceptInterfacesMemory, or
// enumsMemory has been filled before we try to access it. If that is not the case we will ensure the natural Metamodel
// is initialized and when this is done we would expect the memory to contain the entry for our class

val <A : ASTNode>KClass<A>.concept: Concept
    get() = conceptsMemory.getOrPut(this) {
        try {
            val metamodelInstance = requireMetamodelFor(this)
            return metamodelInstance.requireConceptFor(this)
        } catch (t: Throwable) {
            throw RuntimeException("Issue obtaining concept for $this", t)
        }
    }

val <A : Any>KClass<A>.conceptInterface: ConceptInterface
    get() = conceptInterfacesMemory.getOrElse(this) {
        val metamodelInstance = requireMetamodelFor(this)
        return metamodelInstance.requireConceptInterfaceFor(this)
    }

val <E : Enum<*>>KClass<E>.enumeration: Enumeration
    get() = enumsMemory.getOrPut(this) {
        val metamodelInstance = requireMetamodelFor(this)
        return metamodelInstance.requireEnumerationFor(this)
    }

private val conceptsMemory = HashMap<KClass<out ASTNode>, Concept>()
private val conceptInterfacesMemory = HashMap<KClass<out Any>, ConceptInterface>()
private val enumsMemory = HashMap<KClass<out Enum<*>>, Enumeration>()

/**
 * Intended to be used from Java and for built-in classes.
 */
fun recordConceptForClass(clazz: Class<*>, concept: Concept) {
    conceptsMemory[clazz.kotlin as KClass<out ASTNode>] = concept
}

/**
 * Intended to be used from Java and for built-in classes.
 */
fun recordConceptInterfaceForClass(clazz: Class<*>, conceptInterface: ConceptInterface) {
    conceptInterfacesMemory[clazz.kotlin] = conceptInterface
}

fun recordEnumerationForClass(clazz: Class<*>, enumeration: Enumeration) {
    enumsMemory[clazz.kotlin as KClass<out Enum<*>>] = enumeration
}

fun getRecordedConceptInterface(kClass: KClass<out Any>): ConceptInterface? {
    val sl = StarLasuMetamodel.astNode
    val cm = Metamodel?.elements
    return conceptInterfacesMemory[kClass]
}

fun getKnownClassesForConceptInterfaces(): Set<KClass<*>> {
    return conceptInterfacesMemory.keys
}

fun getKnownClassesForEnumerations(): Set<KClass<*>> {
    return enumsMemory.keys
}

fun getRecordedConcept(kClass: KClass<out ASTNode>): Concept? {
    val sl = StarLasuMetamodel.astNode
    val cm = Metamodel?.elements
    return conceptsMemory[kClass]
}

fun getRecordedEnum(kClass: KClass<out Enum<*>>): Enumeration? {
    val sl = StarLasuMetamodel.astNode
    val cm = Metamodel?.elements
    return enumsMemory[kClass]
}

fun isRecorded(kClass: KClass<*>): Boolean {
    return if (kClass.isEnum) {
        getRecordedEnum(kClass as KClass<out Enum<*>>) != null
    } else if (kClass.isInterface) {
        getRecordedConceptInterface(kClass) != null
    } else {
        getRecordedConcept(kClass as KClass<out ASTNode>) != null
    }
}
