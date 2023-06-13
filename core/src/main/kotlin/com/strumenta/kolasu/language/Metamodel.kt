package com.strumenta.kolasu.language

import com.strumenta.kolasu.model.Multiplicity
import kotlin.reflect.KClass
import kotlin.reflect.KType

sealed class Feature {
    abstract val name: String

    /**
     * Only Containment can have Multiplicity Many.
     */
    abstract val multiplicity: Multiplicity
}

data class Attribute(override val name: String, val optional: Boolean, val type: KType) : Feature() {
    override val multiplicity: Multiplicity
        get() = if (optional) Multiplicity.OPTIONAL else Multiplicity.SINGULAR
}

sealed class Link : Feature() {
    abstract val type: KClass<*>
}

data class Reference(override val name: String, val optional: Boolean, override val type: KClass<*>) : Link() {
    override val multiplicity: Multiplicity
        get() = if (optional) Multiplicity.OPTIONAL else Multiplicity.SINGULAR
}

data class Containment(
    override val name: String,
    override val multiplicity: Multiplicity,
    override val type: KClass<*>
) : Link()
