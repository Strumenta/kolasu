package com.strumenta.kolasu.semantics.scoping

/**
 * Abstract storage for scope computation results.
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
 * In-memory storage for scoping results.
 * @property results in-memory map associating inputs to scopes
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
