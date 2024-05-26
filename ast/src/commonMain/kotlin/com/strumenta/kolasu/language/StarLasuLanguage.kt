package com.strumenta.kolasu.language

open class StarLasuLanguage(
    val qualifiedName: String,
    val types: MutableList<Type> = mutableListOf(),
) {
    init {
        StarLasuLanguagesRegistry.registerLanguage(this)
    }

    val simpleName: String
        get() = qualifiedName.split(".").last()

    fun getConceptLike(name: String): Classifier =
        types.filterIsInstance<Classifier>().find {
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

    fun getAnnotation(name: String): Annotation =
        types.filterIsInstance<Annotation>().find { it.name == name || it.qualifiedName == name }
            ?: throw IllegalArgumentException("Cannot find annotation $name in language $this. Known types: $types")

    fun ensureIsRegistered() {
        StarLasuLanguagesRegistry.registerLanguage(this)
    }

    override fun toString(): String = "StarLasuLanguage($qualifiedName)"

    val enums: List<EnumType>
        get() = this.types.filterIsInstance<EnumType>()

    val primitives: List<PrimitiveType>
        get() = this.types.filterIsInstance<PrimitiveType>()

    val classifiers: List<Classifier>
        get() = this.types.filterIsInstance<Classifier>()

    val annotations: List<Annotation>
        get() = this.types.filterIsInstance<Annotation>()

    val concepts: List<Concept>
        get() = this.types.filterIsInstance<Concept>()

    val conceptInterfaces: List<ConceptInterface>
        get() = this.types.filterIsInstance<ConceptInterface>()
}
