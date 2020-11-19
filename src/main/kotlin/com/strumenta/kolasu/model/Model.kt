package com.strumenta.kolasu.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * The Abstract Syntax Tree will be constituted by instances of Node.
 */
@Serializable
open class Node(@Transient open val position: Position? = null) {
    var parent: Node? = null
}

/**
 * This should be used for all relations which are secondary, i.e., they are calculated from other relations
 */
annotation class Derived

/**
 * This will be used to mark all the properties that returns a Node or a list of Node which are not
 * contained by the Node having the properties, they are just references
 */
annotation class Link

/**
 * An entity which has a name.
 */
interface Named {
    val name: String?
}

/**
 * A reference associated by using a name.
 */
@Serializable
data class ReferenceByName<N>(val name: String, var referred: N? = null) where N : Named {
    override fun toString(): String {
        return if (referred == null) {
            "Ref($name)[Unsolved]"
        } else {
            "Ref($name)[Solved]"
        }
    }

    override fun hashCode(): Int {
        return name.hashCode() * (7 + if (referred == null) 1 else 2)
    }

    val resolved: Boolean
        get() = referred != null
}

fun <N> ReferenceByName<N>.tryToResolve(candidates: List<N>, caseInsensitive: Boolean = false): Boolean where N : Named {
    val res = candidates.find { if (it.name == null) false else it.name.equals(this.name, caseInsensitive) }
    this.referred = res
    return res != null
}

fun <N> ReferenceByName<N>.tryToResolve(possibleValue: N?): Boolean where N : Named {
    return if (possibleValue == null) {
        false
    } else {
        this.referred = possibleValue
        true
    }
}
