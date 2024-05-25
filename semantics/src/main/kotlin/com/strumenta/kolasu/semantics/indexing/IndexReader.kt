package com.strumenta.kolasu.semantics.indexing

import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/**
 * Component handling read operations over an index.
 * @property index the underlying index
 **/
class IndexReader(
    private val index: Index = InMemoryIndex()
) {
    /**
     * Retrieve the first value matching [predicate] from the index.
     * @param predicate the predicate to match
     * @return the associated value (exception if none)
     **/
    inline fun <reified T : Any> get(noinline predicate: (T) -> Boolean): T {
        return this.get(T::class, predicate)
    }

    /**
     * Retrieve the first value matching [predicate] from the index.
     * @param predicate the predicate to match
     * @return the associated value (exception if none)
     **/
    @PublishedApi
    internal fun <T : Any> get(type: KClass<T>, predicate: (T) -> Boolean): T {
        return checkNotNull(this.find(type, predicate)) {
            "Cannot find value of type ${type.qualifiedName} matching the given predicate."
        }
    }

    /**
     * Retrieve first value matching [predicate] from the index.
     * @param predicate the predicate to match
     * @return the associated value (null if none)
     **/
    inline fun <reified T : Any> find(noinline predicate: (T) -> Boolean): T? {
        return this.find(T::class, predicate)
    }

    /**
     * Retrieve first value matching [predicate] from the index.
     * @param predicate the predicate to match
     * @return the associated value (null if none)
     **/
    @PublishedApi
    internal fun <T : Any> find(type: KClass<T>, predicate: (T) -> Boolean): T? {
        return this.index
            .find { type.safeCast(it)?.let(predicate) ?: false }
            ?.let { type.safeCast(it) }
    }

    /**
     * Retrieve all values matching [predicate] from the index.
     * @param predicate the predicate to match
     * @return sequence of values matching the given predicate
     **/
    inline fun <reified T : Any> findAll(noinline predicate: (T) -> Boolean = { true }): Sequence<T> {
        return this.findAll(T::class, predicate)
    }

    /**
     * Retrieve all values matching [predicate] from the index.
     * @param predicate the predicate to match
     * @return sequence of values matching the given predicate
     **/
    @PublishedApi
    internal fun <T : Any> findAll(type: KClass<T>, predicate: (T) -> Boolean = { true }): Sequence<T> {
        return this.index
            .findAll { type.safeCast(it)?.let(predicate) ?: false }
            .mapNotNull { type.safeCast(it) }
    }
}
