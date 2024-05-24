package com.strumenta.kolasu.language

import com.strumenta.kolasu.model.Multiplicity
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.ReferenceValue

val RESERVED_FEATURE_NAMES = setOf("parent", "position", "features")

fun checkFeatureName(featureName: String) {
    require(featureName !in RESERVED_FEATURE_NAMES) { "$featureName is not a valid feature name" }
}

sealed class Feature {
    val isMultiple: Boolean
        get() = multiplicity == Multiplicity.MANY

    abstract val name: String

    /**
     * Only Containment can have Multiplicity Many.
     */
    abstract val multiplicity: Multiplicity

    abstract val type: Type

    abstract val derived: Boolean

    abstract val valueProvider: (node: NodeLike) -> Any?

    open fun value(node: NodeLike): Any? = valueProvider.invoke(node)
}

data class Property(
    override val name: String,
    val optional: Boolean,
    override val type: DataType,
    override val valueProvider: (node: NodeLike) -> Any?,
    override val derived: Boolean = false,
) : Feature() {
    init {
        checkFeatureName(name)
    }

    override val multiplicity: Multiplicity
        get() = if (optional) Multiplicity.OPTIONAL else Multiplicity.SINGULAR
}

sealed class Link : Feature() {
    abstract override val type: Classifier
}

data class Reference(
    override val name: String,
    val optional: Boolean,
    override val type: Classifier,
    override val valueProvider: (node: NodeLike) -> ReferenceValue<*>?,
    override val derived: Boolean = false,
) : Link() {
    override val multiplicity: Multiplicity
        get() = if (optional) Multiplicity.OPTIONAL else Multiplicity.SINGULAR

    init {
        checkFeatureName(name)
    }

    override fun value(node: NodeLike): ReferenceValue<*>? = super.value(node) as? ReferenceValue<*>
}

data class Containment(
    override val name: String,
    override val multiplicity: Multiplicity,
    override val type: Classifier,
    override val valueProvider: (node: NodeLike) -> Any?,
    override val derived: Boolean = false,
) : Link() {
    init {
        checkFeatureName(name)
    }

    fun contained(node: NodeLike): List<NodeLike> = node.getChildren(this)
}
