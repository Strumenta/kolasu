package com.strumenta.kolasu.semantics.typing

/**
 * Abstract storage for storing type computation results.
 *
 * Type computation caches are meant to support type computers
 * not to repeat calculations. While a persistent version of a
 * type computer cache can be implemented, it is meant to be
 * volatile by default.
 *
 * Persistent implementations should design dedicated mechanisms
 * for emptying, allocating and deallocating the cache.
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
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
 * In-memory implementation of a type computation cache.
 * @property types in-memory map associating inputs to types
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
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
