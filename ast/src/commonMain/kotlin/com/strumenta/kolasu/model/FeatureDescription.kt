package com.strumenta.kolasu.model

val RESERVED_FEATURE_NAMES = setOf("parent", "position", "features")

fun checkFeatureName(featureName: String) {
    require(featureName !in RESERVED_FEATURE_NAMES) { "$featureName is not a valid feature name" }
}

data class FeatureDescription(
    val name: String,
    val provideNodes: Boolean,
    val multiplicity: Multiplicity,
    val valueProvider: ()->Any?,
    val featureType: FeatureType,
    val derived: Boolean = false
) {
    constructor(
        name: String,
        provideNodes: Boolean,
        multiplicity: Multiplicity,
        value: Any?,
        featureType: FeatureType,
        derived: Boolean = false
    ) : this(name, provideNodes, multiplicity, {value}, featureType, derived)

    val isMultiple: Boolean
        get() = multiplicity == Multiplicity.MANY

    companion object {
    }

    val value: Any?
        get() = valueProvider.invoke()

}
