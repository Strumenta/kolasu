package com.strumenta.kolasu.model

data class FeatureDescription(
    val name: String,
    val provideNodes: Boolean,
    val multiplicity: Multiplicity,
    val value: Any?,
    val featureType: FeatureType,
) {
    val isMultiple: Boolean
        get() = multiplicity == Multiplicity.MANY

    companion object {
    }
}
