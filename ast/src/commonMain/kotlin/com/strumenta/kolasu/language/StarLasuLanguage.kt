package com.strumenta.kolasu.language

import com.strumenta.kolasu.model.MPNode

open class StarLasuLanguage(
    val qualifiedName: String,
    val types: MutableList<Type> = mutableListOf(),
) {
    init {
        StarLasuLanguagesRegistry.registerLanguage(this)
    }

    fun instantiateNode(
        concept: Concept,
        featureValues: Map<Feature, Any?>,
    ): MPNode = concept.instantiateNode(featureValues)

    fun instantiateErrorNode(
        concept: Concept,
        message: String,
    ): MPNode = concept.instantiateErrorNode(message)

    val simpleName: String
        get() = qualifiedName.split(".").last()

    fun getConceptLike(name: String): ConceptLike =
        types.filterIsInstance<ConceptLike>().find {
            it.name == name ||
                it.name == "${this.qualifiedName}.$name" ||
                it.name.split(".").last() == name
        }
            ?: throw IllegalArgumentException(
                "Cannot find ConceptLike named $name. Known types: ${types.joinToString(", "){
                    it
                        .name
                }}",
            )

    fun getConcept(name: String): Concept =
        types.filterIsInstance<Concept>().find { it.name == name || it.qualifiedName == name }
            ?: throw IllegalArgumentException("Cannot find concept $name in language $this. Known types: $types")

    fun getConceptInterface(name: String): ConceptInterface =
        types.filterIsInstance<ConceptInterface>().find {
            it.name == name
        }!!

    fun getDataType(name: String): DataType =
        if (intType.name == name) {
            intType
        } else if (stringType.name == name) {
            stringType
        } else {
            TODO()
        }

    fun getPrimitiveType(name: String): PrimitiveType =
        this.types.filterIsInstance<PrimitiveType>().find {
            it.name == name
        }!!

    fun ensureIsRegistered() {
        StarLasuLanguagesRegistry.registerLanguage(this)
    }

    override fun toString(): String = "StarLasuLanguage($qualifiedName)"

    val enums: List<EnumType>
        get() = this.types.filterIsInstance<EnumType>()

    val primitives: List<PrimitiveType>
        get() = this.types.filterIsInstance<PrimitiveType>()

    val conceptLikes: List<ConceptLike>
        get() = this.types.filterIsInstance<ConceptLike>()
}