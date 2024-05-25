package com.strumenta.kolasu.semantics.scoping

import com.strumenta.kolasu.semantics.indexing.InMemoryIndex
import com.strumenta.kolasu.semantics.indexing.Index
import com.strumenta.kolasu.semantics.indexing.IndexReader
import com.strumenta.kolasu.utils.sortBySubclassesFirst
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/**
 * Type-safe builder to create scope provider instances.
 * @param index optional index for external symbols
 * @param cache optional storage to cache computations
 * @param configuration the scope construction configuration
 * @return the configured scope provider instance
 **/
fun scopeComputer(
    index: Index = InMemoryIndex(),
    cache: ScopeComputerCache = InMemoryScopeComputerCache(),
    configuration: ScopeComputerConfigurator.() -> Unit
): ScopeComputer {
    val rules: MutableMap<KClass<*>, (Any) -> Scope?> = mutableMapOf()
    val scopeComputer = ScopeComputer(index, rules, cache)
    ScopeComputerConfigurator(scopeComputer, rules).apply(configuration)
    return scopeComputer
}

/**
 * Configurable component responsible for the construction of scopes.
 * @property indexReader the index containing external symbols
 * @property rules the scope construction rules to use
 * @property cache local storage to avoid repeating scope constructions
 **/
class ScopeComputer internal constructor(
    index: Index = InMemoryIndex(),
    private val rules: Map<KClass<*>, (Any) -> Scope?>,
    private val cache: ScopeComputerCache = InMemoryScopeComputerCache()
) {
    /**
     * Index to access external symbols.
     **/
    val indexReader: IndexReader = IndexReader(index)

    /**
     * Compute or retrieve the scope for the given [input].
     * @param input the scope construction input
     * @return the corresponding scope (empty if none)
     **/
    fun scopeFrom(input: Any): Scope {
        return this.cache.loadScope(input) ?: this.computeScope(input)
    }

    /**
     * Compute (and cache) the scope for the given [input].
     * @param input the scope construction input
     * @return the corresponding scope (empty if none)
     **/
    private fun computeScope(input: Any): Scope {
        val scope = this.findRule(input)?.invoke(input)
        if (scope != null) { this.cache.storeScope(input, scope) }
        return scope ?: Scope()
    }

    /**
     * Retrieve scope construction rule for the given [input].
     * @param input the scope construction rule input
     * @return the corresponding scope construction rule
     **/
    private fun findRule(input: Any): ((Any) -> Scope?)? {
        return this.rules.keys.filter { it.isInstance(input) }
            .sortBySubclassesFirst().firstOrNull()?.let { this.rules[it] }
    }
}

/**
 * Rule-based configurator of a scope provider.
 * @property scopeComputer the configured scope provider
 * @property rules the rules of this configuration
 **/
class ScopeComputerConfigurator internal constructor(
    private val scopeComputer: ScopeComputer,
    private val rules: MutableMap<KClass<*>, (Any) -> Scope?>
) {
    /**
     * Define the scope construction [rule] for [input].
     * @param input the input type of the scope construction rule
     * @param rule the scope construction rule definition
     **/
    fun <T : Any> scopeFrom(input: KClass<T>, rule: ScopeComputer.(T) -> Scope?) = apply {
        this.rules[input] = { input.safeCast(it)?.let { input -> scopeComputer.rule(input) } }
    }
}
