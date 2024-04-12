package com.strumenta.kolasu.semantics.symbol.repository

import com.strumenta.kolasu.semantics.symbol.description.SymbolDescription

/**
 * Generic storage interface for [SymbolDescription] instances.
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
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
     * Retrieves all symbols from the repository.
     * @param predicate optional filter over the symbols
     * @return sequence of symbols in the repository
     **/
    fun all(predicate: (SymbolDescription) -> Boolean = { true }): Sequence<SymbolDescription>

    /**
     * Removes all symbols from the repository.
     **/
    fun clear()
}
