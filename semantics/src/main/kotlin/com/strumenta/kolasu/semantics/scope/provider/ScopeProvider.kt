package com.strumenta.kolasu.semantics.scope.provider

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.semantics.scope.description.ScopeDescription
import com.strumenta.kolasu.semantics.symbol.description.SymbolDescription
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

// TODO documentation

data class ReferenceNode<TargetTy : PossiblyNamed>(
    val type: KClass<TargetTy>,
    val reference: ReferenceByName<TargetTy>
) : Node() {
    constructor(name: String, type: KClass<TargetTy>, referred: TargetTy? = null) :
        this(type, ReferenceByName(name, referred))
}

fun scopeProvider(init: ScopeProviderConfigurationApi.() -> Unit): ScopeProvider {
    return ConfigurableScopeProvider().apply(init)
}

interface ScopeProvider {
    fun <TargetTy> scopeFor(
        node: ReferenceNode<TargetTy>,
        issues: MutableList<Issue> = mutableListOf(),
        typedAs: KClass<in TargetTy>? = null
    ): ScopeDescription where TargetTy : PossiblyNamed
}

@DslMarker
annotation class ScopeProviderDsl

@ScopeProviderDsl
interface ScopeProviderConfigurationApi {
    fun <TargetTy> scopeFor(
        reference: KClass<TargetTy>,
        ignoreCase: Boolean = false,
        rule: ScopeProviderConfigurationRuleApi<TargetTy>.(ScopeProviderConfigurationRuleContext<TargetTy>) -> Unit
    ) where TargetTy : PossiblyNamed
}

@ScopeProviderDsl
interface ScopeProviderConfigurationRuleApi<TargetTy : PossiblyNamed> {

    fun info(message: String)

    fun warning(message: String)

    fun error(message: String)

    fun <SymbolTy> defineLocalSymbol(symbol: SymbolTy)
        where SymbolTy : Node, SymbolTy : PossiblyNamed

    fun defineLocalSymbol(name: String, symbol: Node)

    fun defineExternalSymbol(symbol: SymbolDescription)

    fun defineExternalSymbol(name: String, symbol: SymbolDescription)

    fun defineExternalSymbol(name: String, identifier: String)

    fun parent(scope: ScopeDescription)

    fun parent(
        configuration: ScopeProviderConfigurationRuleApi<TargetTy>.(
            ScopeProviderConfigurationRuleContext<TargetTy>
        ) -> Unit
    )
}

private class ConfigurableScopeProvider : ScopeProviderConfigurationApi, ScopeProvider {

    private val rules: MutableList<ConfigurableScopeProviderRule<*>> = mutableListOf()

    override fun <TargetTy : PossiblyNamed> scopeFor(
        reference: KClass<TargetTy>,
        ignoreCase: Boolean,
        rule: ScopeProviderConfigurationRuleApi<TargetTy>.(ScopeProviderConfigurationRuleContext<TargetTy>) -> Unit
    ) {
        this.rules.add(ConfigurableScopeProviderRule(ignoreCase, reference, rule))
    }

    override fun <TargetTy : PossiblyNamed> scopeFor(
        node: ReferenceNode<TargetTy>,
        issues: MutableList<Issue>,
        typedAs: KClass<in TargetTy>?
    ): ScopeDescription {
        return this.rules.sorted()
            .firstOrNull { it.isCompatibleWith(typedAs ?: node.type) }
            ?.invoke(this, node, issues) ?: ScopeDescription()
    }
}

private class ConfigurableScopeProviderRule<TargetTy : PossiblyNamed>(
    private val ignoreCase: Boolean = false,
    val targetTy: KClass<TargetTy>,
    private val configuration: ScopeProviderConfigurationRuleApi<TargetTy>.(
        ScopeProviderConfigurationRuleContext<TargetTy>
    ) -> Unit
) : ScopeProviderConfigurationRuleApi<TargetTy>,
    (ScopeProvider, ReferenceNode<*>, MutableList<Issue>) -> ScopeDescription,
    Comparable<ConfigurableScopeProviderRule<*>> {

    private lateinit var context: ScopeProviderConfigurationRuleContext<TargetTy>
    private lateinit var issues: MutableList<Issue>
    private lateinit var scopeDescription: ScopeDescription

    override fun info(message: String) {
        this.issue(message, IssueSeverity.INFO)
    }

    override fun warning(message: String) {
        this.issue(message, IssueSeverity.WARNING)
    }

    override fun error(message: String) {
        this.issue(message, IssueSeverity.ERROR)
    }

    private fun issue(message: String, severity: IssueSeverity) {
        this.issues.add(Issue.semantic(message, severity, this.context.node.position))
    }

    override fun <SymbolTy> defineLocalSymbol(symbol: SymbolTy) where SymbolTy : Node, SymbolTy : PossiblyNamed {
        this.scopeDescription.defineLocalSymbol(symbol)
    }

    override fun defineLocalSymbol(name: String, symbol: Node) {
        this.scopeDescription.defineLocalSymbol(name, symbol)
    }

    override fun defineExternalSymbol(symbol: SymbolDescription) {
        this.scopeDescription.defineExternalSymbol(symbol)
    }

    override fun defineExternalSymbol(name: String, symbol: SymbolDescription) {
        this.scopeDescription.defineExternalSymbol(name, symbol)
    }

    override fun defineExternalSymbol(name: String, identifier: String) {
        this.scopeDescription.defineExternalSymbol(name, identifier)
    }

    override fun parent(scope: ScopeDescription) {
        this.scopeDescription.parentScope = scope
    }

    override fun parent(
        configuration: ScopeProviderConfigurationRuleApi<TargetTy>
        .(ScopeProviderConfigurationRuleContext<TargetTy>) -> Unit
    ) {
        ConfigurableScopeProviderRule(this.ignoreCase, this.targetTy, configuration)
            .invoke(this.context.scopeProvider, this.context.node, this.issues).let(this::parent)
    }

    fun isCompatibleWith(targetType: KClass<*>): Boolean {
        return this.targetTy.isSuperclassOf(targetType)
    }

    override fun invoke(
        scopeProvider: ScopeProvider,
        node: ReferenceNode<*>,
        issues: MutableList<Issue>
    ): ScopeDescription {
        check(this.isCompatibleWith(node.type)) {
            "Error while running scope provider rule - incompatible node received"
        }
        @Suppress("UNCHECKED_CAST")
        this.context = ScopeProviderConfigurationRuleContext(node as ReferenceNode<TargetTy>, scopeProvider)
        this.issues = issues
        this.scopeDescription = ScopeDescription(this.ignoreCase)
        this.configuration(this.context)
        return this.scopeDescription
    }

    override fun compareTo(other: ConfigurableScopeProviderRule<*>): Int {
        return when {
            this.targetTy.isSuperclassOf(other.targetTy) -> 1
            other.targetTy.isSuperclassOf(this.targetTy) -> -1
            else -> (this.targetTy.qualifiedName ?: "") compareTo (other.targetTy.qualifiedName ?: "")
        }
    }
}

data class ScopeProviderConfigurationRuleContext<TargetTy : PossiblyNamed>(
    val node: ReferenceNode<TargetTy>,
    val scopeProvider: ScopeProvider
)
