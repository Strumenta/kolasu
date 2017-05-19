package me.tomassetti.kolasu.model

import java.util.*


/**
 * The Abstract Syntax Tree will be constituted by instances of Node.
 */
interface Node {
    val position: Position?
}

/**
 * This will be used to mark all the properties that returns a Node or a list of Node which are not
 * contained by the Node having the properties.
 */
annotation class Derived

///
/// Named
///

interface Named {
    val name: String
}

///
/// References
///

data class ReferenceByName<N>(val name: String, var referred: N? = null) where N : Named {
    override fun toString(): String {
        if (referred == null) {
            return "Ref($name)[Unsolved]"
        } else {
            return "Ref($name)[Solved]"
        }
    }

    override fun hashCode(): Int {
        return name.hashCode() * (7 + if (referred == null) 1 else 2)
    }

    val resolved : Boolean
        get() = referred != null
}

fun <N> ReferenceByName<N>.tryToResolve(candidates: List<N>) : Boolean where N : Named {
    val res = candidates.find { it.name == this.name }
    this.referred = res
    return res != null
}

fun <N> ReferenceByName<N>.tryToResolve(possibleValue: N?) : Boolean where N : Named {
    if (possibleValue == null) {
        return false
    } else {
        this.referred = possibleValue
        return true
    }
}



