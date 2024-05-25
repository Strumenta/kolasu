package com.strumenta.kolasu.semantics.indexing

import com.strumenta.kolasu.utils.sortBySubclassesFirst
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/**
 * Type-safe builder to create index writer instances.
 * @param configuration the index writer configuration
 * @return the configured index writer instance
 **/
fun indexWriter(
    index: Index = InMemoryIndex(),
    configuration: IndexWriterConfigurator.() -> Unit
) = mutableMapOf<KClass<*>, (Any) -> Any?>()
    .also { rules -> IndexWriterConfigurator(rules).apply(configuration) }
    .let { rules -> IndexWriter(index, rules) }

/**
 * Configurable component handling write operations over an index.
 * @property index the underlying index
 * @property rules the index writer rules to use
 **/
class IndexWriter(
    private val index: Index = InMemoryIndex(),
    private val rules: Map<KClass<*>, (Any) -> Any?> = emptyMap()
) {
    /**
     * Execute the index writer rule for the given [input].
     * @param input the index writer rule input
     * @return the output of the corresponding rule
     **/
    fun write(input: Any): Any? {
        return this.findRule(input)?.invoke(input)?.also { this.index.insert(it) }
    }

    /**
     * Retrieve index writer rule for the given [input].
     * @param input the index writer rule input
     * @return the corresponding index writer rule
     **/
    private fun findRule(input: Any): ((Any) -> Any?)? {
        return this.rules.keys.filter { it.isInstance(input) }
            .sortBySubclassesFirst().firstOrNull()?.let { this.rules[it] }
    }
}

/**
 * Rule-based configurator of an index writer.
 * @property rules the rules of this configuration
 **/
class IndexWriterConfigurator(
    private val rules: MutableMap<KClass<*>, (Any) -> Any?>
) {
    /**
     * Define the index writer [rule] for [input].
     * @param input the input type of the index writer rule
     * @param rule the index writer rule definition
     **/
    fun <T : Any> write(input: KClass<T>, rule: ((T) -> Any?)? = null) = apply {
        this.rules[input] = {
            input.safeCast(it)
                ?.let { inputValue -> rule?.invoke(inputValue) ?: inputValue }
        }
    }
}
