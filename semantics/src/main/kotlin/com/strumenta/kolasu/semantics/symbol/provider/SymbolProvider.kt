package com.strumenta.kolasu.semantics.symbol.provider

import com.strumenta.kolasu.ids.NodeIdProvider
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.semantics.symbol.description.*
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.isSuperclassOf

// TODO documentation

fun symbolProvider(
    nodeIdProvider: NodeIdProvider,
    init: SymbolProviderConfigurationApi.() -> Unit
): SymbolProvider {
    return ConfigurableSymbolProvider(nodeIdProvider).apply(init)
}

interface SymbolProvider {
    fun <NodeTy : Node> symbolFor(
        node: NodeTy,
        issues: MutableList<Issue> = mutableListOf(),
        typedAs: KClass<in NodeTy>? = null
    ): SymbolDescription?
}

@DslMarker
annotation class SymbolProviderDsl

@SymbolProviderDsl
interface SymbolProviderConfigurationApi {
    fun <NodeTy : Node> symbolFor(
        nodeType: KClass<out NodeTy>,
        rule: SymbolProviderConfigurationRuleApi.(SymbolProviderConfigurationRuleContext<out NodeTy>) -> Unit
    )
}

@SymbolProviderDsl
interface SymbolProviderConfigurationRuleApi {
    fun info(message: String)

    fun warning(message: String)

    fun error(message: String)

    fun include(propertyName: String, propertyValue: Any?)
}

private class ConfigurableSymbolProvider(
    private val nodeIdProvider: NodeIdProvider
) : SymbolProviderConfigurationApi, SymbolProvider {

    private val rules: MutableList<ConfigurableSymbolProviderRule<*>> = mutableListOf()

    override fun <NodeTy : Node> symbolFor(
        nodeType: KClass<out NodeTy>,
        rule: SymbolProviderConfigurationRuleApi.(SymbolProviderConfigurationRuleContext<out NodeTy>) -> Unit
    ) {
        this.rules.add(ConfigurableSymbolProviderRule(this.nodeIdProvider, nodeType, rule))
    }

    override fun <NodeTy : Node> symbolFor(
        node: NodeTy,
        issues: MutableList<Issue>,
        typedAs: KClass<in NodeTy>?
    ): SymbolDescription? {
        return this.rules.sorted()
            .firstOrNull { it.isCompatibleWith(typedAs ?: node::class) }
            ?.invoke(this, node, issues)
    }
}

private class ConfigurableSymbolProviderRule<NodeTy : Node>(
    private val nodeIdProvider: NodeIdProvider,
    private val nodeType: KClass<NodeTy>,
    private val configuration: SymbolProviderConfigurationRuleApi.(
        SymbolProviderConfigurationRuleContext<NodeTy>
    ) -> Unit
) : SymbolProviderConfigurationRuleApi,
    (SymbolProvider, Node, MutableList<Issue>) -> SymbolDescription,
    Comparable<ConfigurableSymbolProviderRule<*>> {

    private lateinit var context: SymbolProviderConfigurationRuleContext<NodeTy>
    private lateinit var issues: MutableList<Issue>

    private var name: String? = null
    private val fields: MutableMap<String, ValueDescription> = mutableMapOf()

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

    override fun include(propertyName: String, propertyValue: Any?) {
        propertyName.takeIf { it == "name" }?.also { this.name = propertyValue as? String }
        this.fields[propertyName] = this.toValueDescription(propertyValue)
    }

    override fun compareTo(other: ConfigurableSymbolProviderRule<*>): Int {
        return when {
            this.nodeType.isSuperclassOf(other.nodeType) -> 1
            other.nodeType.isSuperclassOf(this.nodeType) -> -1
            else -> (this.nodeType.qualifiedName ?: "") compareTo (other.nodeType.qualifiedName ?: "")
        }
    }

    fun isCompatibleWith(nodeType: KClass<*>): Boolean {
        return this.nodeType.isSuperclassOf(nodeType)
    }

    override fun invoke(
        symbolProvider: SymbolProvider,
        node: Node,
        issues: MutableList<Issue>
    ): SymbolDescription {
        check(this.isCompatibleWith(node::class)) {
            "Error while running symbol provider rule - incompatible node received"
        }
        @Suppress("UNCHECKED_CAST")
        this.context = SymbolProviderConfigurationRuleContext(node as NodeTy, symbolProvider)
        this.issues = issues
        this.configuration(this.context)
        return this.createSymbolDescriptionFor(node)
    }

    private fun createSymbolDescriptionFor(node: NodeTy) = SymbolDescription(
        name = this.getName(),
        identifier = this.nodeIdProvider.id(node),
        types = this.computeTypeFor(node),
        fields = this.fields
    )

    private fun getName(): String {
        check(this.name != null) {
            "Error while running symbol provider rule - symbol description name cannot be null"
        }
        return this.name!!
    }

    private fun computeTypeFor(node: NodeTy): List<String> {
        return listOfNotNull(node::class.qualifiedName)
            .plus(node::class.allSuperclasses.mapNotNull(KClass<*>::qualifiedName))
    }

    private fun toValueDescription(source: Any?): ValueDescription {
        return when (source) {
            is Boolean -> BooleanValueDescription(source)
            is Int -> IntegerValueDescription(source)
            is String -> StringValueDescription(source)
            is Node -> toContainmentValueDescription(source)
            is ReferenceByName<*> -> toReferenceValueDescription(source)
            is List<*> -> toListValueDescription(source)
            null -> NullValueDescription
            else -> throw RuntimeException("Unsupported value description for ${source::class.qualifiedName}")
        }
    }

    private fun toReferenceValueDescription(source: ReferenceByName<*>): ReferenceValueDescription {
        return ReferenceValueDescription(source.referred?.let { it as? Node }?.let { this.nodeIdProvider.id(it) })
    }

    private fun toContainmentValueDescription(source: Node): ContainmentValueDescription {
        return ContainmentValueDescription(this.nodeIdProvider.id(source))
    }

    private fun toListValueDescription(source: List<*>): ListValueDescription {
        return ListValueDescription(source.map { this.toValueDescription(it) }.toList())
    }
}

data class SymbolProviderConfigurationRuleContext<NodeTy : Node>(val node: NodeTy, val symbolProvider: SymbolProvider)
