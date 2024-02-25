package com.strumenta.kolasu.kcp

import com.strumenta.kolasu.model.BaseNode
import com.strumenta.kolasu.model.FeatureDescription
import com.strumenta.kolasu.model.FeatureType
import com.strumenta.kolasu.model.Multiplicity

data class A(val f: String) : BaseNode()

fun main(args: Array<String>) {
    val n = A("foo")
    FeatureDescription("a", false, Multiplicity.MANY, {n.f}, FeatureType.ATTRIBUTE, false)
}