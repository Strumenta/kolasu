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
}

class ConceptInterface : ConceptLike() {
    var superInterfaces: MutableList<ConceptInterface> = mutableListOf()
    override val superConceptLikes: List<ConceptLike>
        get() = superInterfaces
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
}
