package com.strumenta.kolasu.semantics.linking

import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.semantics.indexing.InMemoryIndex
import com.strumenta.kolasu.semantics.indexing.Index
import com.strumenta.kolasu.semantics.indexing.IndexReader
import com.strumenta.kolasu.utils.sortBySubclassesFirst
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.safeCast

/**
 * Type-safe builder to create resolver instances.
 * @param index index for external symbols (default: [InMemoryIndex])
 * @param cache storage to cache computations (default: [InMemoryReferenceResolverCache])
 * @param configuration the resolver configuration
 * @return the configured resolver instance
 **/
fun referenceResolver(
    index: Index = InMemoryIndex(),
    cache: ReferenceResolverCache = InMemoryReferenceResolverCache(),
    configuration: ReferenceResolverConfigurator.() -> Unit
): ReferenceResolver {
    val rules = mutableMapOf<String, MutableMap<KClass<*>, (Pair<*, String>) -> Any?>>()
    val referenceResolver = ReferenceResolver(index, cache, rules)
    ReferenceResolverConfigurator(referenceResolver, rules).apply(configuration)
    return referenceResolver
}

/**
 * Configurable component responsible for the resolution of symbols.
 * @property indexReader the index containing external symbols
 * @property rules the symbol resolution rules to use
 * @property cache local storage to avoid repeating symbol resolutions
 **/
class ReferenceResolver internal constructor(
    index: Index = InMemoryIndex(),
    private val cache: ReferenceResolverCache = InMemoryReferenceResolverCache(),
    private val rules: Map<String, Map<KClass<*>, (Pair<*, String>) -> Any?>>
) {
    /**
     * Index to access external symbols.
     **/
    val indexReader: IndexReader = IndexReader(index)

    /**
     * Resolve the given [container].[reference].
     * @param container the reference container
     * @param reference the reference definition
     * @return the (possibly cached) resolution result (null if none)
     **/
    fun <C : Any> resolve(container: C, reference: KProperty1<in C, ReferenceByName<*>?>) =
        this.cache.load(container, reference) ?: this.resolveReference(container, reference)

    /**
     * Resolve the given [container].[reference].
     * @param container the reference container
     * @param reference the reference definition
     * @return the resolution result (null if none)
     **/
    private fun <C : Any> resolveReference(container: C, reference: KProperty1<in C, ReferenceByName<*>?>) =
        this.invokeRule(container, reference)?.also { target -> this.cache.store(container, reference, target) }

    /**
     * Invoke resolution rule for [container].[reference].
     * @param container the reference container
     * @param reference the reference definition
     * @return the resolution result (null if none)
     **/
    private fun <C : Any> invokeRule(container: C, reference: KProperty1<in C, ReferenceByName<*>?>) =
        reference.get(container)?.name
            ?.let { name -> container to name }
            ?.let { input -> this.findRule(container, reference)?.invoke(input) }

    /**
     * Retrieve resolution rule for [container].[reference].
     * @param container the reference container
     * @param reference the reference definition
     * @return the corresponding resolution rule
     **/
    private fun <C : Any> findRule(container: C, reference: KProperty1<in C, ReferenceByName<*>?>) =
        this.rules[reference.name]
            ?.let { referenceRules ->
                referenceRules.keys
                    .filter { it.isInstance(container) }
                    .sortBySubclassesFirst().firstOrNull()?.let { referenceRules[it] }
            }
}

/**
 * Rule-based configurator of resolvers.
 * @property referenceResolver the configured resolver
 * @property rules the rules of this configuration
 **/
class ReferenceResolverConfigurator internal constructor(
    private val referenceResolver: ReferenceResolver,
    private val rules: MutableMap<String, MutableMap<KClass<*>, (Pair<*, String>) -> Any?>>
) {
    /**
     * Define the resolution [rule] for [reference].
     * @param reference the reference to resolve
     * @param rule the resolution rule
     **/
    inline fun <reified C : Any, T : PossiblyNamed> resolve(
        reference: KProperty1<C, ReferenceByName<T>?>,
        noinline rule: ReferenceResolver.(Pair<C, String>) -> T?
    ) = this.resolve(C::class, reference, rule)

    /**
     * Define the resolution [rule] for [reference].
     * @param container the reference container type
     * @param reference the reference to resolve
     * @param rule the resolution rule
     **/
    @PublishedApi
    internal fun <C : Any, T : PossiblyNamed> resolve(
        container: KClass<C>,
        reference: KProperty1<C, ReferenceByName<T>?>,
        rule: ReferenceResolver.(Pair<C, String>) -> T?
    ): ReferenceResolverConfigurator = apply { this.registerRule(container, reference, rule) }

    /**
     *  Register resolution [rule] for [reference].
     *  @param container the reference container type
     *  @param reference the reference to resolve
     *  @param rule the resolution logic
     **/
    private fun <C : Any, T : PossiblyNamed> registerRule(
        container: KClass<C>,
        reference: KProperty1<C, ReferenceByName<T>?>,
        rule: ReferenceResolver.(Pair<C, String>) -> T?
    ) {
        rules.getOrPut(reference.name) { mutableMapOf() }[container] = { input ->
            container.safeCast(input.first)?.let { it to input.second }?.let { referenceResolver.rule(it) }
        }
    }
}
