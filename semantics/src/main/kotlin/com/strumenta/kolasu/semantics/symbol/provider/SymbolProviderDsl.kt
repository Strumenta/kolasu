package com.strumenta.kolasu.semantics.symbol.provider

import com.strumenta.kolasu.ids.NodeIdProvider
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.semantics.symbol.description.*
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.isSuperclassOf

@DslMarker
annotation class SymbolProviderDsl

fun symbolProvider(
    nodeIdProvider: NodeIdProvider,
    init: SymbolProviderConfigurationApi.() -> Unit
): SymbolProvider {
    return ConfigurableSymbolProvider(nodeIdProvider).apply(init)
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

    override fun symbolFor(node: Node): SymbolDescription? {
        return this.rules.sorted()
            .firstOrNull { it.isCompatibleWith(node::class) }
            ?.invoke(this, node)
    }
}

private class ConfigurableSymbolProviderRule<NodeTy : Node>(
    private val nodeIdProvider: NodeIdProvider,
    private val nodeType: KClass<NodeTy>,
    private val init: SymbolProviderConfigurationRuleApi.(SymbolProviderConfigurationRuleContext<NodeTy>) -> Unit
) : SymbolProviderConfigurationRuleApi,
    (SymbolProvider, Node) -> SymbolDescription,
    Comparable<ConfigurableSymbolProviderRule<*>> {

    private var name: String? = null
    private val properties: MutableMap<String, ValueDescription> = mutableMapOf()

    override fun include(name: String, value: Any?) {
        name.takeIf { it == "name" }?.also { this.name = value as? String }
        this.properties[name] = this.toValueDescription(value)
    }

    fun isCompatibleWith(nodeType: KClass<*>): Boolean {
        return this.nodeType.isSuperclassOf(nodeType)
    }

    // !!! this should be called iff 'isCompatibleWith' returns true
    override fun invoke(symbolProvider: SymbolProvider, node: Node): SymbolDescription {
        @Suppress("UNCHECKED_CAST")
        return (node as NodeTy).let {
            this.init(SymbolProviderConfigurationRuleContext(node, symbolProvider))
            SymbolDescription(
                this.getName(node),
                nodeIdProvider.id(node),
                this.getTypes(node),
                this.properties
            )
        }
    }

    override fun compareTo(other: ConfigurableSymbolProviderRule<*>): Int {
        return when {
            this.nodeType.isSuperclassOf(other.nodeType) -> 1
            other.nodeType.isSuperclassOf(this.nodeType) -> -1
            else -> (this.nodeType.qualifiedName ?: "") compareTo (other.nodeType.qualifiedName ?: "")
        }
    }

    private fun getName(node: NodeTy): String {
        return this.name ?: throw RuntimeException(
            "Symbol description name property not set for node: ${node::class.qualifiedName}"
        )
    }

    private fun getTypes(node: NodeTy): List<String> {
        return listOfNotNull(
            node::class.qualifiedName
        ) + node::class.allSuperclasses.mapNotNull(KClass<*>::qualifiedName)
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
