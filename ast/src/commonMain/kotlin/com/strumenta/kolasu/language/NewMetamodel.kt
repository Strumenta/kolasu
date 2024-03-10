package com.strumenta.kolasu.language

import com.strumenta.kolasu.model.MPNode
import kotlin.reflect.KClass

class StarLasuLanguage {
    fun conceptForClass(kClass: KClass<*>): Concept {
        TODO()
    }

    fun instantiateNode(
        concept: Concept,
        featureValues: Map<Feature, Any?>,
    ): MPNode {
        TODO()
    }

    fun instantiateErrorNode(
        concept: Concept,
        message: String,
    ): MPNode {
        TODO()
    }
}

sealed class ConceptLike {
    abstract val superConceptLikes: List<ConceptLike>
    val features: MutableList<Feature> = mutableListOf()
    abstract val allFeatures: List<Feature>
    val allAttributes: List<Attribute>
        get() = allFeatures.filterIsInstance<Attribute>()

    fun feature(name: String): Feature? = allFeatures.find { it.name == name }

    fun attribute(name: String): Attribute? = allAttributes.find { it.name == name }
}

class ConceptInterface : ConceptLike() {
    var superInterfaces: MutableList<ConceptInterface> = mutableListOf()
    override val superConceptLikes: List<ConceptLike>
        get() = superInterfaces
    override val allFeatures: List<Feature>
        get() = TODO("Not yet implemented")
}

class Concept(
    var name: String,
) : ConceptLike() {
    var superConcept: Concept? = null
    var conceptInterfaces: MutableList<ConceptInterface> = mutableListOf()

    override val superConceptLikes: List<ConceptLike>
        get() = if (superConcept == null) conceptInterfaces else listOf(superConcept!!) + conceptInterfaces

    val language: StarLasuLanguage
        get() = TODO()

    fun instantiateNode(featureValues: Map<Feature, Any?>): MPNode {
        TODO()
    }

    fun instantiateErrorNode(message: String): MPNode {
        TODO()
    }

    override fun equals(other: Any?): Boolean {
        // TODO consider language too
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Concept

        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()

    override val allFeatures: List<Feature>
        get() = TODO("Not yet implemented")
}
