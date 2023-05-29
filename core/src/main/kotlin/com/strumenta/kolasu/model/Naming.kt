package com.strumenta.kolasu.model

import java.io.Serializable

/**
 * An entity that can have a name
 */
interface PossiblyNamed {
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

/**
 * A reference associated by using a name.
 * It can be used only to refer to Nodes and not to other values.
 *
 * This is not statically enforced as we may want to use some interface, which cannot extend Node.
 * However, this is enforced dynamically.
 */
class ReferenceByName<N>(val name: String, initialReferred: N? = null) : Serializable where N : PossiblyNamed {

    var referred: N? = null
        set(value) {
            require(value is Node || value == null) {
                "We cannot enforce it statically but only Node should be referred to. Instead $value was assigned " +
                    "(class: ${value?.javaClass})"
            }
            field = value
        }

    init {
        this.referred = initialReferred
    }

    override fun toString(): String {
        return if (isResolved) {
            "Ref($name)[Solved]"
        } else {
            "Ref($name)[Unsolved]"
        }
    }

    val isResolved: Boolean
        get() = referred != null

    override fun hashCode(): Int {
        return name.hashCode() * (7 + if (isResolved) 2 else 1)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReferenceByName<*>) return false

        if (name != other.name) return false
        if (referred != other.referred) return false

        return true
    }
}

/**
 * Try to resolve the reference by finding a named element with a matching name.
 * The name match is performed in a case sensitive or insensitive way depending on the value of @param[caseInsensitive].
 */
fun <N> ReferenceByName<N>.tryToResolve(
    candidates: Iterable<N>,
    caseInsensitive: Boolean = false
): Boolean where N : PossiblyNamed {
    val res: N? = candidates.find { if (it.name == null) false else it.name.equals(this.name, caseInsensitive) }
    this.referred = res
    return res != null
}

/**
 * Try to resolve the reference by assigining @param[possibleValue]. The assignment is not performed if
 * @param[possibleValue] is null.
 *
 * @return true if the assignment has been performed
 */
fun <N> ReferenceByName<N>.tryToResolve(possibleValue: N?): Boolean where N : PossiblyNamed {
    return if (possibleValue == null) {
        false
    } else {
        this.referred = possibleValue
        true
    }
}
