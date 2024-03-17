package com.strumenta.kolasu.model

/**
 * An entity that can have a name
 */
interface PossiblyNamed : NodeLike {
    /**
     * The optional name of the entity.
     */
    val name: String?
}

/**
 * An entity which has a name.
 */
interface Named : PossiblyNamed {
    /**
     * The mandatory name of the entity.
     */
    override val name: String
}
