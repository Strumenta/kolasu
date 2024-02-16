package com.strumenta.kolasu.semantics

import com.strumenta.kolasu.model.KReferenceByName
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.getReferredType
import com.strumenta.kolasu.model.kReferenceByNameProperties
import com.strumenta.kolasu.traversing.walkChildren
import kotlin.reflect.KClass

// instance

class SymbolResolver(
    private val scopeProvider: ScopeProvider = ScopeProvider(),
) {
    fun loadFrom(
        configuration: SymbolResolverConfiguration,
        semantics: Semantics,
    ) {
        this.scopeProvider.loadFrom(configuration.scopeProvider, semantics)
    }

    @Suppress("unchecked_cast")
    fun resolve(node: NodeLike) {
        node.kReferenceByNameProperties().forEach { this.resolve(it as KReferenceByName<out NodeLike>, node) }
        node.walkChildren().forEach(this::resolve)
    }

    @Suppress("unchecked_cast")
    fun resolve(
        property: KReferenceByName<out NodeLike>,
        node: NodeLike,
    ) {
        (node.features.find { it.name == property.name }?.value as ReferenceByName<PossiblyNamed>?)?.apply {
            this.referred = scopeProvider.scopeFor(property, node).resolve(this.name, property.getReferredType())
        }
    }

    fun scopeFor(
        property: KReferenceByName<out NodeLike>,
        node: NodeLike? = null,
    ): Scope = this.scopeProvider.scopeFor(property, node)

    fun scopeFrom(node: NodeLike? = null): Scope = this.scopeProvider.scopeFrom(node)
}

// configuration

class SymbolResolverConfiguration(
    val scopeProvider: ScopeProviderConfiguration = ScopeProviderConfiguration(),
) {
    inline fun <reified N : NodeLike> scopeFor(
        referenceByName: KReferenceByName<N>,
        crossinline scopeResolutionRule: Semantics.(N) -> Scope,
    ) = this.scopeProvider.scopeFor(referenceByName, scopeResolutionRule)

    inline fun <reified N : NodeLike> scopeFrom(
        nodeType: KClass<N>,
        crossinline scopeConstructionRule: Semantics.(N) -> Scope,
    ) = this.scopeProvider.scopeFrom(nodeType, scopeConstructionRule)
}

// builder

fun symbolResolver(init: SymbolResolverConfiguration.() -> Unit) = SymbolResolverConfiguration().apply(init)
