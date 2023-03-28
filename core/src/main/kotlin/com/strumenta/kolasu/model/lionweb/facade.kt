package com.strumenta.kolasu.model.lionweb

import com.strumenta.kolasu.model.ASTNode
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
        val metamodelInstance = requireMetamodelFor(this)
        return metamodelInstance.requireConceptFor(this)
    }

val <E : Enum<*>>KClass<E>.enumeration: Enumeration
    get() = enumsMemory.getOrPut(this) {
        // calculateEnum(this)
        TODO()
    }


val <A : Any>KClass<A>.conceptInterface: ConceptInterface
    get() = conceptInterfacesMemory.getOrElse(this) {
        //calculateConceptInterface(this, conceptsMemory, conceptInterfacesMemory)
        TODO()
    }


private val conceptsMemory = HashMap<KClass<out ASTNode>, Concept>()
private val enumsMemory = HashMap<KClass<out Enum<*>>, Enumeration>()
private val conceptInterfacesMemory = HashMap<KClass<out Any>, ConceptInterface>()