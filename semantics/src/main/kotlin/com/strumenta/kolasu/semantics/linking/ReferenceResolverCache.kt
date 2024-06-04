package com.strumenta.kolasu.semantics.linking

import com.strumenta.kolasu.model.ReferenceByName
import kotlin.reflect.KProperty1

/**
 * Abstract storage for storing reference resolution results.
 *
 * Reference resolution caches are meant to support the reference
 * resolver not to repeat calculations. While a persistent version
 * of a reference resolver cache can be implemented, it is meant
 * to be volatile by default.
 *
 * Persistent implementations should design dedicated
 * mechanisms for emptying, allocating and deallocating the cache.
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
interface ReferenceResolverCache {
    /**
     * Load resolution result for [container].[reference].
     * @param container the entity containing the reference
     * @param reference the reference definition
     * @return the associated resolution result (null if none)
     **/
    fun <C : Any> load(container: C, reference: KProperty1<in C, ReferenceByName<*>?>): Any?

    /**
     * Store resolution [target] for [container].[reference].
     * @param container the entity containing the reference
     * @param reference the reference definition
     * @param target the result of resolving [container].[reference]
     **/
    fun <C : Any> store(container: C, reference: KProperty1<in C, ReferenceByName<*>?>, target: Any)
}

/**
 * In-memory implementation of a reference resolution cache.
 * @property results in-memory map associating container-references to targets
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
class InMemoryReferenceResolverCache(
    private val results: MutableMap<Any, MutableMap<String, Any>> = mutableMapOf()
) : ReferenceResolverCache {
    override fun <C : Any> load(container: C, reference: KProperty1<in C, ReferenceByName<*>?>): Any? {
        return this.results[container]?.get(reference.name)
    }

    override fun <C : Any> store(container: C, reference: KProperty1<in C, ReferenceByName<*>?>, target: Any) {
        this.results.getOrPut(container) { mutableMapOf() }[reference.name] = target
    }
}
