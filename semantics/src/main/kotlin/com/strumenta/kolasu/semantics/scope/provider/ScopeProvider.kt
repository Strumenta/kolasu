package com.strumenta.kolasu.semantics.scope.provider

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.semantics.provider.SemanticsProvider
import com.strumenta.kolasu.semantics.provider.SemanticsProviderConfigurator
import com.strumenta.kolasu.semantics.provider.SemanticsProviderRule
import com.strumenta.kolasu.semantics.scope.description.ScopeDescription
import com.strumenta.kolasu.validation.Issue
import kotlin.reflect.KClass

/**
 * Type-safe builder to create [ScopeProvider] instances.
 *
 * @param init configuration of the scope provider
 * @return a [ScopeProvider] instance realising the specified rules
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
fun scopeProvider(init: ScopeProviderConfigurator.() -> Unit): ScopeProvider {
    return ScopeProvider().apply { ScopeProviderConfigurator(this).init() }
}

/**
 * Annotation class grouping elements of the Scope Provider
 * DSL - used to configure [ScopeProvider] instances.
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
@DslMarker
annotation class ScopeProviderDsl

/**
 * Query-side representation of a [ScopeProvider] associating
 * [Node] instances to the corresponding [ScopeDescription].
 *
 * @see SemanticsProvider
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
class ScopeProvider : SemanticsProvider<ScopeDescription, ScopeProviderRule<*>>()

/**
 * Configuration-side representation of a [ScopeProvider]
 * supporting the declarative specification of scoping rules.
 *
 * @param scopeProvider the configured [ScopeProvider] instance
 * @see SemanticsProviderConfigurator
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
@ScopeProviderDsl
class ScopeProviderConfigurator(
    scopeProvider: ScopeProvider
) : SemanticsProviderConfigurator<ScopeProvider, ScopeProviderRule<*>, ScopeDescription>(scopeProvider) {
    override fun <NodeTy : Node> createRule(nodeType: KClass<NodeTy>): ScopeProviderRule<*> {
        return ScopeProviderRule<NodeTy>()
    }
}

/**
 * Scope provider rule definition exposing the configuration API
 * for single scoping rules and handling the actual evaluation.
 *
 * @see SemanticsProviderRule
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
@ScopeProviderDsl
class ScopeProviderRule<NodeTy : Node> : SemanticsProviderRule<NodeTy, ScopeDescription>() {

    /**
     * Output scope description populated by the rule.
     **/
    private var scopeDescription: ScopeDescription? = ScopeDescription()

    /**
     * Parent scope description
     **/
    var parent: ScopeDescription?
        get() = this.scopeDescription?.parent
        set(value) { this.scopeDescription?.parent = value }

    /**
     * Flag indicating whether the scope description should consider casing.
     **/
    var ignoreCase: Boolean
        get() = this.scopeDescription?.ignoreCase ?: false
        set(value) { this.scopeDescription?.ignoreCase = value }

    /**
     * Includes the given [name]-[symbol] association in the scope description.
     *
     * @param symbol the symbol to include
     * @param name the name to use (possibly inferred from [symbol])
     **/
    fun include(symbol: Any?, name: String? = null) {
        this.scopeDescription?.include(symbol, name)
    }

    /**
     * Sets the parent scope description from an explicit configuration.
     * @param init the parent scope configuration
     **/
    fun parent(init: ScopeProviderRule<NodeTy>.() -> Unit) = runBeforeEvaluation { (input, provider, issues) ->
        this.parent = ScopeProviderRule<NodeTy>().apply(init).evaluate(input, provider, issues)
    }

    /**
     * Delegates the scope description configuration to the rule defined for [node].
     * @param node the node to use for delegation
     **/
    fun from(node: Node?) = runBeforeEvaluation { (_, provider) ->
        this.scopeDescription = node?.let { provider.getFor(it) }
    }

    override fun getOutput(
        node: NodeTy,
        provider: SemanticsProvider<ScopeDescription, *>,
        issues: MutableList<Issue>
    ) = this.scopeDescription
}
