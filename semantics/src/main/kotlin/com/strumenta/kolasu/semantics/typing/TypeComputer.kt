package com.strumenta.kolasu.semantics.typing

import com.strumenta.kolasu.semantics.indexing.InMemoryIndex
import com.strumenta.kolasu.semantics.indexing.Index
import com.strumenta.kolasu.semantics.indexing.IndexReader
import com.strumenta.kolasu.utils.sortBySubclassesFirst
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/**
 * Type-safe builder to create type computer instances.
 * @param index optional index for external symbols
 * @param cache optional storage to cache computations
 * @param configuration the type computation configuration
 * @return the configured type computer instance
 **/
fun typeComputer(
    index: Index = InMemoryIndex(),
    cache: TypeComputerCache = InMemoryTypeComputerCache(),
    configuration: TypeComputerConfigurator.() -> Unit
): TypeComputer {
    val rules: MutableMap<KClass<*>, (Any) -> Any?> = mutableMapOf()
    val typeComputer = TypeComputer(index, rules, cache)
    TypeComputerConfigurator(typeComputer, rules).apply(configuration)
    return typeComputer
}

/**
 * Configurable component responsible for the computation of types.
 * @property indexReader the index containing external symbols
 * @property rules the type computation rules to use
 * @property cache local storage to avoid repeating type computations
 **/
class TypeComputer internal constructor(
    index: Index = InMemoryIndex(),
    private val rules: Map<KClass<*>, (Any) -> Any?>,
    private val cache: TypeComputerCache = InMemoryTypeComputerCache()
) {
    /**
     * Index to access external symbols.
     **/
    val indexReader: IndexReader = IndexReader(index)

    /**
     * Compute or retrieve the type for the given [input].
     * @param input the type computation input
     * @return the corresponding type (null if none)
     **/
    fun typeFor(input: Any): Any? {
        return this.cache.loadType(input) ?: this.computeType(input)
    }

    /**
     * Compute (and cache) the scope for the given [input].
     * @param input the type computation input
     * @return the corresponding input (null if none)
     **/
    private fun computeType(input: Any): Any? {
        val type = this.findRule(input)?.invoke(input)
        if (type != null) { this.cache.storeType(input, type) }
        return type
    }

    /**
     * Retrieve type computation rule for the given [input].
     * @param input the type computation rule input
     * @return the corresponding type computation rule
     **/
    private fun findRule(input: Any): ((Any) -> Any?)? {
        return this.rules.keys.filter { it.isInstance(input) }
            .sortBySubclassesFirst().firstOrNull()?.let { this.rules[it] }
    }
}

/**
 * Rule-based configurator for a type computer.
 * @property typeComputer the configured type computer
 * @property rules the rules of this configuration
 **/
class TypeComputerConfigurator internal constructor(
    private val typeComputer: TypeComputer,
    private val rules: MutableMap<KClass<*>, (Any) -> Any?>
) {
    /**
     * Define the type computation [rule] for [input].
     * @param input the input type of the type computation rule
     * @param rule the type computation rule definition
     **/
    fun <T : Any> typeFor(input: KClass<T>, rule: TypeComputer.(T) -> Any?) = apply {
        this.rules[input] = { input.safeCast(it)?.let { input -> typeComputer.rule(input) } }
    }
}
