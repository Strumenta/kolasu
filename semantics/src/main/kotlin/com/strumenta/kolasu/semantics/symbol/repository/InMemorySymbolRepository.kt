package com.strumenta.kolasu.semantics.symbol.repository

import com.strumenta.kolasu.semantics.symbol.description.SymbolDescription

/**
 * In-memory implementation of a [SymbolRepository] where symbols are kept
 * inside a [Map] from [String] identifiers to [SymbolDescription] instances.
 *
 * @property symbols the underlying symbols [Map].
 **/
class InMemorySymbolRepository(
    private val symbols: MutableMap<String, SymbolDescription> = mutableMapOf()
) : SymbolRepository {

    override fun store(symbol: SymbolDescription) {
        this.symbols[symbol.identifier] = symbol
    }

    override fun load(identifier: String): SymbolDescription? {
        return this.symbols[identifier]
    }

    override fun delete(identifier: String): Boolean {
        return this.symbols.remove(identifier) != null
    }

    override fun all(predicate: (SymbolDescription) -> Boolean): Sequence<SymbolDescription> {
        return this.symbols.values.asSequence().filter(predicate)
    }

    override fun clear() {
        this.symbols.clear()
    }
}
