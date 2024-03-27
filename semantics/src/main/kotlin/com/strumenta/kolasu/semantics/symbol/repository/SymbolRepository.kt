package com.strumenta.kolasu.semantics.symbol.repository

import com.strumenta.kolasu.semantics.symbol.description.SymbolDescription
import kotlin.reflect.KClass

interface SymbolRepository {

    /**
     * Stores the [symbol] in the repository.
     * @param symbol the symbol to store in the repository
     **/
    fun store(symbol: SymbolDescription)

    /**
     * Retrieves the symbol with the given [identifier] if present.
     * @param identifier the symbol identifier
     * @return the symbol with the given [identifier]
     **/
    fun load(identifier: String): SymbolDescription?

    /**
     * Deletes the symbol with the given [identifier] if present.
     * @param identifier the symbol identifier
     * @return true if the symbol has been deleted successfully
     **/
    fun delete(identifier: String): Boolean

    /**
     * Retrieves all symbols from the repository with an optional [filter].
     * @param filter optional filter over the elements
     * @return sequence of symbols matching the [filter]
     **/
    fun loadAll(filter: (SymbolDescription) -> Boolean = { true }): Sequence<SymbolDescription>

    /**
     * Retrieves all symbols for the given [nodeType].
     * @param nodeType the node type to look for
     * @return sequence of symbols for nodes of the given [nodeType]
     **/
    fun findAll(nodeType: KClass<*>) =
        this.loadAll { symbolDescription -> symbolDescription.type.isSubTypeOf(nodeType) }

    /**
     * Removes all symbols from the repository.
     **/
    fun clear()
}
