package com.strumenta.kolasu.semantics.scoping

/**
 * Abstract storage for storing scope computation results.
 *
 * Scope computation caches are meant to support the scope
 * computer not to repeat calculations. While a persistent
 * version of a scope computation cache can be implemented,
 * it is meant to be volatile by default.
 *
 * Persistent implementations should design dedicated
 * mechanisms for emptying, allocating and deallocating the cache.
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
interface ScopeComputerCache {
    /**
     * Load scope computation result.
     * @param input the input of the scope computation
     * @return the associated scope computation result (null if none)
     **/
    fun loadScope(input: Any): Scope?

    /**
     * Store scope computation result.
     * @param input the input of the scope computation
     * @param scope the scope computation result to store
     **/
    fun storeScope(input: Any, scope: Scope)
}

/**
 * In-memory implementation of a scope computation cache.
 * @property results in-memory map associating inputs to scopes
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
class InMemoryScopeComputerCache(
    private val results: MutableMap<Any, Scope> = mutableMapOf()
) : ScopeComputerCache {
    override fun loadScope(input: Any): Scope? {
        return this.results[input]
    }

    override fun storeScope(input: Any, scope: Scope) {
        this.results[input] = scope
    }
}
