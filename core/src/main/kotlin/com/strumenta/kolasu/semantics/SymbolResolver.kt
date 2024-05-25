package com.strumenta.kolasu.semantics

import com.strumenta.kolasu.model.*
import com.strumenta.kolasu.traversing.walkChildren
import kotlin.reflect.KClass

// instance
class SymbolResolver(
    private val scopeProvider: ScopeProvider = ScopeProvider()
) {
    fun loadFrom(configuration: SymbolResolverConfiguration, semantics: Semantics) {
        this.scopeProvider.loadFrom(configuration.scopeProvider, semantics)
    }

    @Suppress("unchecked_cast")
    fun resolve(node: Node) {
        node.kReferenceByNameProperties().forEach { this.resolve(it as KReferenceByName<out Node>, node) }
        node.walkChildren().forEach(this::resolve)
    }

    @Suppress("unchecked_cast")
    fun resolve(property: KReferenceByName<out Node>, node: Node) {
        (node.properties.find { it.name == property.name }?.value as ReferenceByName<PossiblyNamed>?)?.apply {
            this.referred = scopeProvider.scopeFor(property, node).resolve(this.name, property.getReferredType())
        }
    }

    fun scopeFor(property: KReferenceByName<out Node>, node: Node? = null): Scope {
        return this.scopeProvider.scopeFor(property, node)
    }

    fun scopeFrom(node: Node? = null): Scope {
        return this.scopeProvider.scopeFrom(node)
    }
}

// configuration
class SymbolResolverConfiguration(
    val scopeProvider: ScopeProviderConfiguration = ScopeProviderConfiguration()
) {
    inline fun <reified N : Node> scopeFor(
        referenceByName: KReferenceByName<N>,
        crossinline scopeResolutionRule: Semantics.(N) -> Scope
    ) = this.scopeProvider.scopeFor(referenceByName, scopeResolutionRule)

    inline fun <reified N : Node> scopeFrom(
        nodeType: KClass<N>,
        crossinline scopeConstructionRule: Semantics.(N) -> Scope
    ) = this.scopeProvider.scopeFrom(nodeType, scopeConstructionRule)
}

// builder

fun symbolResolver(init: SymbolResolverConfiguration.() -> Unit) = SymbolResolverConfiguration().apply(init)
