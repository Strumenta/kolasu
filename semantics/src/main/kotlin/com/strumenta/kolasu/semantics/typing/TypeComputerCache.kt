package com.strumenta.kolasu.semantics.typing

/**
 * Abstract storage for type computation results.
 **/
interface TypeComputerCache {
    /**
     * Load type from table.
     * @param input the input of the type computation
     * @return the associated type computation result (null if none)
     **/
    fun loadType(input: Any): Any?

    /**
     * Store type into table.
     * @param input the source of type
     * @param type the type to store
     **/
    fun storeType(input: Any, type: Any)
}

/**
 * In-memory storage for type computation results.
 **/
class InMemoryTypeComputerCache(
    private val types: MutableMap<Any, Any> = mutableMapOf()
) : TypeComputerCache {
    override fun loadType(input: Any): Any? {
        return this.types[input]
    }

    override fun storeType(input: Any, type: Any) {
        this.types[input] = type
    }
}
