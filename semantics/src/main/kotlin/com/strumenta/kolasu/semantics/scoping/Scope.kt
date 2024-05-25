package com.strumenta.kolasu.semantics.scoping

import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/**
 * Type-safe builder for scope instances.
 * @param configuration the scope configuration
 * @return the configured scope instance
 **/
fun scope(configuration: ScopeConfigurator.() -> Unit): Scope {
    val parents: MutableList<Scope> = mutableListOf()
    val entries: MutableMap<String, MutableList<Any>> = mutableMapOf()
    ScopeConfigurator(parents, entries).apply(configuration)
    return Scope(parents, entries)
}

/**
 * Scope component maintaining information about what
 * is visible at a given point - e.g. node, symbol or else.
 * @property parents the parent/previous scopes of this scope
 * @property entries the elements contained in this scope (key-value)
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
data class Scope(
    private val parents: List<Scope> = emptyList(),
    private val entries: Map<String, List<Any>> = emptyMap()
) {
    /**
     * Retrieve entries matching the given predicate and type.
     * @param predicate the predicate used to filter entries
     * @return sequence of entries matching the given predicate
     **/
    inline fun <reified T : Any> entries(
        noinline predicate: (Pair<String, T>) -> Boolean = { true }
    ) = sequence { allEntries(T::class).filter(predicate).forEach { yield(it.second) } }

    /**
     * Retrieve all entries from this scope and its parents.
     * @param type the type of the entries to retrieve
     * @return sequence of entries contained in this scope and its parents.
     **/
    @PublishedApi
    internal fun <T : Any> allEntries(type: KClass<T>): Sequence<Pair<String, T>> = sequence {
        entries.asSequence()
            .flatMap { (name, values) ->
                values
                    .mapNotNull { value -> type.safeCast(value) }
                    .map { value -> name to value }
            }.forEach { yield(it) }
        parents.asSequence()
            .flatMap { parent -> parent.allEntries(type) }
            .forEach { yield(it) }
    }
}

/**
 * Configurator of scope instances.
 * @property scope the configured scope
 * @property parents the parent scopes of the configured scope
 * @property entries the entries of the configured scope
 **/
class ScopeConfigurator internal constructor(
    val parents: MutableList<Scope> = mutableListOf(),
    private val entries: MutableMap<String, MutableList<Any>> = mutableMapOf()
) {
    /**
     * Define entry in the underlying scope.
     * @param name the name of the entry.
     * @param value the value of the entry.
     **/
    fun define(name: String, value: Any) {
        this.entries.getOrPut(name) { mutableListOf() }.add(value)
    }
}
