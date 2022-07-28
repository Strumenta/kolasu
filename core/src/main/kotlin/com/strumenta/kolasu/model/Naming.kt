package com.strumenta.kolasu.model

import kotlin.reflect.KClass

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

/**
 * Try to resolve the reference by finding a named element with a matching name.
 * The name match is performed in a case sensitive or insensitive way depending on the value of @param[caseInsensitive].
 */
fun <N> ReferenceByName<N>.tryToResolve(
    candidates: List<N>,
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

interface Symbol : PossiblyNamed

class Scope private constructor(
    private val symbols: MutableMap<String, MutableList<Symbol>> = mutableMapOf(),
    private val parent: Scope? = null,
) {
    constructor(vararg symbols: Symbol, parent: Scope? = null) : this(symbols = listOf(*symbols), parent = parent)

    constructor(symbols: List<Symbol> = emptyList(), parent: Scope? = null) : this(
        symbols = symbols.groupBy { it.name ?: throw IllegalArgumentException("All given symbols must have a name") }
            .mapValues { it.value.toMutableList() }.toMutableMap(),
        parent = parent
    )

    fun add(symbol: Symbol) {
        val symbolName: String = symbol.name ?: throw IllegalArgumentException("The given symbol must have a name")
        this.symbols.computeIfAbsent(symbolName) { mutableListOf() }.add(symbol)
    }

    fun lookup(symbolName: String, symbolType: KClass<*> = Symbol::class): Symbol? {
        return this.symbols.getOrDefault(symbolName, mutableListOf()).find { symbolType.isInstance(it) }
            ?: this.parent?.lookup(symbolName, symbolType)
    }

    fun getSymbols(): Map<String, List<Symbol>> {
        return this.symbols
    }
}
