package com.strumenta.kolasu.language

import com.strumenta.kolasu.model.Multiplicity
import com.strumenta.kolasu.model.checkFeatureName

data class StarLasuLanguage(
    val qualifiedName: String,
    val types: MutableList<Type> = mutableListOf(),
) {
    val simpleName: String
        get() = qualifiedName.split(".").last()
}

// Types

sealed class Type(
    open val name: String,
)

sealed class ConceptLike(
    name: String,
    val features: MutableList<Feature> = mutableListOf(),
) : Type(name)

class Concept(
    name: String,
    features: MutableList<Feature> = mutableListOf(),
) : ConceptLike(name, features)

class ConceptInterface(
    name: String,
    features: MutableList<Feature> = mutableListOf(),
) : ConceptLike(name, features)

sealed class DataType(
    name: String,
) : Type(name)

data class PrimitiveType(
    override val name: String,
) : DataType(name)

data class EnumType(
    override val name: String,
    val literals: MutableList<EnumerationLiteral> = mutableListOf(),
) : DataType(name)

data class EnumerationLiteral(
    val name: String,
)

// Features

sealed class Feature {
    abstract val name: String

    /**
     * Only Containment can have Multiplicity Many.
     */
    abstract val multiplicity: Multiplicity

    abstract val type: Type
}

data class Attribute(
    override val name: String,
    val optional: Boolean,
    override val type: DataType,
) : Feature() {
    init {
//        require(!type.isMarkedNullable) {
//            "The type should be specified as not nullable. " +
//                "The optional flag should be used to represent nullability"
//        }
        checkFeatureName(name)
    }

    override val multiplicity: Multiplicity
        get() = if (optional) Multiplicity.OPTIONAL else Multiplicity.SINGULAR
}

sealed class Link : Feature() {
    abstract override val type: ConceptLike
}

data class Reference(
    override val name: String,
    val optional: Boolean,
    override val type: ConceptLike,
) : Link() {
    override val multiplicity: Multiplicity
        get() = if (optional) Multiplicity.OPTIONAL else Multiplicity.SINGULAR

    init {
        checkFeatureName(name)
    }
}

data class Containment(
    override val name: String,
    override val multiplicity: Multiplicity,
    override val type: ConceptLike,
) : Link() {
    init {
        checkFeatureName(name)
    }
}
