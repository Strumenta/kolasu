package com.strumenta.kolasu.language

import com.strumenta.kolasu.model.MPNode
import kotlin.reflect.KClass

sealed class Classifier(
    val language: StarLasuLanguage,
    override var name: String,
) : Type(name) {
    abstract val superClassifiers: List<Classifier>
    val declaredFeatures: MutableList<Feature> = mutableListOf()
    val allFeatures: List<Feature>
        get() {
            val res = mutableListOf<Feature>()
            res.addAll(declaredFeatures)
            this.superClassifiers.forEach { scl ->
                res.addAll(scl.allFeatures)
            }
            return res
        }
    val allProperties: List<Property>
        get() = allFeatures.filterIsInstance<Property>()
    val allContainments: List<Containment>
        get() = allFeatures.filterIsInstance<Containment>()
    val allReferences: List<Reference>
        get() = allFeatures.filterIsInstance<Reference>()

    fun feature(name: String): Feature? = allFeatures.find { it.name == name }

    fun property(name: String): Property? = allProperties.find { it.name == name }

    fun containment(name: String): Containment? = allContainments.find { it.name == name }

    fun reference(name: String): Reference? = allReferences.find { it.name == name }

    fun requireProperty(name: String): Property =
        property(name) ?: throw IllegalArgumentException("Cannot find property $name in ${this.name}")

    fun requireContainment(name: String): Containment =
        containment(name) ?: throw IllegalArgumentException("Cannot find containment $name in ${this.name}")

    fun requireReference(name: String): Reference =
        reference(name) ?: throw IllegalArgumentException("Cannot find reference $name in ${this.name}")

    val qualifiedName: String
        get() = "${language.qualifiedName}.$name"
}

class ConceptInterface(
    language: StarLasuLanguage,
    name: String,
) : Classifier(language, name) {
    var superInterfaces: MutableList<ConceptInterface> = mutableListOf()
    override val superClassifiers: List<Classifier>
        get() = superInterfaces

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Concept

        return name == other.name && this.language == other.language
    }

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = "ConceptInterface(${this.qualifiedName})"
}

class Concept(
    language: StarLasuLanguage,
    name: String,
) : Classifier(language, name) {
    var superConcept: Concept? = null
    var conceptInterfaces: MutableList<ConceptInterface> = mutableListOf()
    var isAbstract: Boolean = false
    var explicitlySetKotlinClass: KClass<*>? = null

    override val superClassifiers: List<Classifier>
        get() = if (superConcept == null) conceptInterfaces else listOf(superConcept!!) + conceptInterfaces

    fun instantiateNode(featureValues: Map<Feature, Any?>): MPNode {
        TODO()
    }

    fun instantiateErrorNode(message: String): MPNode {
        TODO()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Concept

        return name == other.name && this.language == other.language
    }

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = "Concept(${this.qualifiedName})"
}

class Annotation(
    language: StarLasuLanguage,
    name: String,
) : Classifier(language, name) {
    var superAnnotation: Annotation? = null
    var conceptInterfaces: MutableList<ConceptInterface> = mutableListOf()
    var annotates: Classifier? = null

    override val superClassifiers: List<Classifier>
        get() = if (superAnnotation == null) conceptInterfaces else listOf(superAnnotation!!) + conceptInterfaces
}
