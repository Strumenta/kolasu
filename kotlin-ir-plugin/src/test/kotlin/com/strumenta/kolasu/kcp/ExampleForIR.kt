package com.strumenta.kolasu.kcp

import com.strumenta.kolasu.model.BaseNode
import com.strumenta.kolasu.model.FeatureDescription
import com.strumenta.kolasu.model.FeatureType
import com.strumenta.kolasu.model.GenericFeatureDescription
import com.strumenta.kolasu.model.Multiplicity

data class A(
    val f: String,
) : BaseNode() {
    private fun helperF(): Any? = this.f

    override fun calculateFeatures(): List<FeatureDescription> =
        mutableListOf(
            FeatureDescription("a", false, Multiplicity.MANY, ::helperF, FeatureType.ATTRIBUTE, false),
        )
}

fun main(args: Array<String>) {
    val n = A("foo")
    GenericFeatureDescription("a", false, Multiplicity.MANY, { it -> (it as A).f }, FeatureType.ATTRIBUTE, false)
}
