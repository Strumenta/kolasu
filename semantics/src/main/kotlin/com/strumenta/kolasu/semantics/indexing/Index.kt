package com.strumenta.kolasu.semantics.indexing

/**
 * Abstract storage component for external symbols.
 **/
interface Index {
    /**
     * Retrieve first value matching [predicate] from the index.
     * @param predicate the predicate to match
     * @return the associated value (exception if none)
     **/
    fun get(predicate: (Any) -> Boolean): Any {
        return checkNotNull(find(predicate)) {
            "Cannot find value matching the given predicate."
        }
    }

    /**
     * Retrieve first value matching [predicate] from the index.
     * @param predicate the predicate to match
     * @return the associated value (null if none)
     **/
    fun find(predicate: (Any) -> Boolean): Any?

    /**
     * Retrieve all values matching [predicate] from the index.
     * @param predicate the predicate to match
     * @return sequence of values matching the given predicate
     **/
    fun findAll(predicate: (Any) -> Boolean = { true }): Sequence<Any>

    /**
     * Remove value from the index.
     * @param value the value to remove
     **/
    fun remove(value: Any)

    /**
     * Store value in the index.
     * @param value the value to insert
     **/
    fun insert(value: Any)
}

/**
 * In-memory storage component for external symbols.
 * @property values the index values
 **/
class InMemoryIndex(
    private val values: MutableList<Any> = mutableListOf()
) : Index {
    override fun find(predicate: (Any) -> Boolean): Any? {
        return this.values.firstOrNull(predicate)
    }

    override fun findAll(predicate: (Any) -> Boolean): Sequence<Any> {
        return this.values.asSequence().filter(predicate)
    }

    override fun remove(value: Any) {
        this.values.remove(value)
    }

    override fun insert(value: Any) {
        this.remove(value)
        this.values.add(value)
    }
}
