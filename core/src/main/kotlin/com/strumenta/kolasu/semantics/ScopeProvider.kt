package com.strumenta.kolasu.semantics

import com.strumenta.kolasu.model.KReferenceByName
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.utils.memoize
import com.strumenta.kolasu.utils.sortBySubclassesFirst
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

// instance

class ScopeProvider(
    private val scopeResolutionRules: MutableMap<String,
        MutableMap<KClass<out Node>, (Node) -> Scope>> = mutableMapOf(),
    private val scopeConstructionRules: MutableMap<KClass<out Node>, (Node) -> Scope> = mutableMapOf()
) {
    fun loadFrom(configuration: ScopeProviderConfiguration, semantics: Semantics) {
        configuration.scopeResolutionRules.mapValuesTo(this.scopeResolutionRules) {
                (_, classToScopeResolutionRules) ->
            classToScopeResolutionRules
                .mapValues { (_, scopeResolutionRule) ->
                    { node: Node -> semantics.scopeResolutionRule(node) }
                }.toMutableMap()
        }
        configuration.scopeConstructionRules.mapValuesTo(this.scopeConstructionRules) {
                (_, scopeConstructionRule) ->
            { node: Node -> semantics.scopeConstructionRule(node) }
        }
    }

    fun scopeFor(referenceByName: KReferenceByName<out Node>, node: Node? = null): Scope {
        return node
            ?.let { this.scopeResolutionRules.getOrDefault(referenceByName.name, null) }
            ?.let {
                it.keys
                    .filter { kClass -> kClass.isSuperclassOf(node::class) }
                    .sortBySubclassesFirst()
                    .firstOrNull()
                    ?.let { kClass -> it[kClass] }
                    ?.invoke(node)
            }
            ?: Scope()
    }

    fun scopeFrom(node: Node? = null): Scope {
        return node?.let {
            this.scopeConstructionRules.keys
                .filter { it.isSuperclassOf(node::class) }
                .sortBySubclassesFirst()
                .firstOrNull()
        }?.let { this.scopeConstructionRules[it] }?.invoke(node) ?: Scope()
    }
}

// configuration

class ScopeProviderConfiguration(
    val scopeResolutionRules: MutableMap<String,
        MutableMap<KClass<out Node>, Semantics.(Node) -> Scope>> = mutableMapOf(),
    val scopeConstructionRules: MutableMap<KClass<out Node>, Semantics.(Node) -> Scope> = mutableMapOf()
) {
    inline fun <reified N : Node> scopeFor(
        referenceByName: KReferenceByName<N>,
        crossinline scopeResolutionRule: Semantics.(N) -> Scope
    ) {
        this.scopeResolutionRules
            .getOrPut(referenceByName.name) { mutableMapOf() }
            .putIfAbsent(
                N::class,
                { semantics: Semantics, node: Node ->
                    if (node is N) semantics.scopeResolutionRule(node) else Scope()
                }.memoize()
            )
    }

    inline fun <reified N : Node> scopeFrom(
        nodeType: KClass<N>,
        crossinline scopeConstructionRule: Semantics.(N) -> Scope
    ) {
        this.scopeConstructionRules.putIfAbsent(
            nodeType,
            { semantics: Semantics, node: Node ->
                if (node is N) semantics.scopeConstructionRule(node) else Scope()
            }.memoize()
        )
    }
}

// builder

fun scopeProvider(init: ScopeProviderConfiguration.() -> Unit) = ScopeProviderConfiguration().apply(init)

// scopes

// TODO handle multiple symbols (e.g. function overloading)
// TODO allow other than name-based symbol binding (e.g. predicated, numbered, etc.)
data class Scope(
    var parent: Scope? = null,
    val symbolTable: MutableMap<String, MutableList<PossiblyNamed>> = mutableMapOf(),
    val ignoreCase: Boolean = false
) {
    fun define(symbol: PossiblyNamed) {
        this.symbolTable.computeIfAbsent(symbol.name.toSymbolTableKey()) { mutableListOf() }.add(symbol)
    }

    fun resolve(name: String? = null, type: KClass<out PossiblyNamed> = PossiblyNamed::class): PossiblyNamed? {
        val key = name.toSymbolTableKey()
        return this.symbolTable.getOrDefault(key, mutableListOf()).find { type.isInstance(it) }
            ?: this.parent?.resolve(key, type)
    }

    private fun String?.toSymbolTableKey() = when {
        this != null && ignoreCase -> this.lowercase()
        this != null -> this
        else -> throw IllegalArgumentException("The given symbol must have a name")
    }
}
