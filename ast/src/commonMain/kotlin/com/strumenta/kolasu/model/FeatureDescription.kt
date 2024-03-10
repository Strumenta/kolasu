package com.strumenta.kolasu.model

val RESERVED_FEATURE_NAMES = setOf("parent", "position", "features")

fun checkFeatureName(featureName: String) {
    require(featureName !in RESERVED_FEATURE_NAMES) { "$featureName is not a valid feature name" }
}

class FeatureDescription(
    val name: String,
    val multiplicity: Multiplicity,
    val valueProvider: () -> Any?,
    val featureType: FeatureType,
    val derived: Boolean = false,
) {
    constructor(
        name: String,
        multiplicity: Multiplicity,
        value: Any?,
        featureType: FeatureType,
        derived: Boolean = false,
    ) : this(name, multiplicity, { value }, featureType, derived)

    @Deprecated("Consider the feature type")
    val provideNodes: Boolean
        get() = featureType in arrayOf(FeatureType.CONTAINMENT)

    val isMultiple: Boolean
        get() = multiplicity == Multiplicity.MANY

    companion object {
    }

    val value: Any?
        get() = (valueProvider ?: throw IllegalStateException()).invoke()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as FeatureDescription

        if (name != other.name) return false
        if (provideNodes != other.provideNodes) return false
        if (multiplicity != other.multiplicity) return false
        if (featureType != other.featureType) return false
        if (derived != other.derived) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + provideNodes.hashCode()
        result = 31 * result + multiplicity.hashCode()
        result = 31 * result + featureType.hashCode()
        result = 31 * result + derived.hashCode()
        return result
    }
}
