package com.strumenta.kolasu.semantics.scope.provider

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.semantics.provider.SemanticsProvider
import com.strumenta.kolasu.semantics.provider.SemanticsProviderConfigurator
import com.strumenta.kolasu.semantics.provider.SemanticsProviderRule
import com.strumenta.kolasu.semantics.scope.description.ScopeDescription
import com.strumenta.kolasu.validation.Issue
import kotlin.reflect.KClass

// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

fun scopeProvider(init: ScopeProviderConfigurator.() -> Unit): ScopeProvider {
    return ScopeProvider().apply { ScopeProviderConfigurator(this).init() }
}

// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@DslMarker
annotation class ScopeProviderDsl

// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class ScopeProvider : SemanticsProvider<ScopeDescription, ScopeProviderRule<*>>()

// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@ScopeProviderDsl
class ScopeProviderConfigurator(
    scopeProvider: ScopeProvider
) : SemanticsProviderConfigurator<ScopeProvider, ScopeProviderRule<*>, ScopeDescription>(scopeProvider) {
    override fun <InputType : Node> createRule(nodeType: KClass<InputType>): ScopeProviderRule<*> {
        return ScopeProviderRule<InputType>()
    }
}

// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@ScopeProviderDsl
class ScopeProviderRule<InputType : Node> : SemanticsProviderRule<InputType, ScopeDescription>() {

    private var scopeDescription: ScopeDescription? = ScopeDescription()

    var parent: ScopeDescription?
        get() = this.scopeDescription?.parent
        set(value) { this.scopeDescription?.parent = value }

    var ignoreCase: Boolean
        get() = this.scopeDescription?.ignoreCase ?: false
        set(value) { this.scopeDescription?.ignoreCase = value }

    fun include(symbol: Any?, name: String? = null) {
        this.scopeDescription?.include(symbol, name)
    }

    fun parent(init: ScopeProviderRule<InputType>.() -> Unit) = runBeforeEvaluation { (input, provider, issues) ->
        this.parent = ScopeProviderRule<InputType>().apply(init).evaluate(input, provider, issues)
    }

    fun from(node: Node?) = runBeforeEvaluation { (_, provider) ->
        this.scopeDescription = node?.let { provider.from(it) }
    }

    override fun getOutput(
        input: InputType,
        provider: SemanticsProvider<ScopeDescription, *>,
        issues: MutableList<Issue>
    ) = this.scopeDescription
}

// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
