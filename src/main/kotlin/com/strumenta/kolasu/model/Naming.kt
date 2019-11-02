package com.strumenta.kolasu.model

/**
 * An entity that could have a name
 */
interface PossiblyNamed {
    val name: String?
}

/**
 * An entity which has a name.
 */
interface Named : PossiblyNamed {
    override val name: String
}

/**
 * A reference associated by using a name.
 */
data class ReferenceByName<N>(val name: String, var referred: N? = null) where N : PossiblyNamed {
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

fun <N> ReferenceByName<N>.tryToResolve(candidates: List<N>, caseInsensitive: Boolean = false): Boolean where N : PossiblyNamed {
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
