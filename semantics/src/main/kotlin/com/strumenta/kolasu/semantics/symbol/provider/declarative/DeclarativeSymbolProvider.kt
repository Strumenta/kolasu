package com.strumenta.kolasu.semantics.symbol.provider.declarative

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.semantics.identifier.provider.IdentifierProvider
import com.strumenta.kolasu.semantics.symbol.description.BooleanValueDescription
import com.strumenta.kolasu.semantics.symbol.description.ContainmentValueDescription
import com.strumenta.kolasu.semantics.symbol.description.IntegerValueDescription
import com.strumenta.kolasu.semantics.symbol.description.ListValueDescription
import com.strumenta.kolasu.semantics.symbol.description.ReferenceValueDescription
import com.strumenta.kolasu.semantics.symbol.description.StringValueDescription
import com.strumenta.kolasu.semantics.symbol.description.SymbolDescription
import com.strumenta.kolasu.semantics.symbol.description.ValueDescription
import com.strumenta.kolasu.semantics.symbol.provider.SymbolProvider
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.isSuperclassOf

inline fun <reified NodeTy : Node> symbolFor(
    noinline specification: DeclarativeSymbolProvideRuleApi<NodeTy>.(
        DeclarativeSymbolProviderRuleContext<NodeTy>
    ) -> Unit
): DeclarativeSymbolProviderRule<NodeTy> {
    return DeclarativeSymbolProviderRule(NodeTy::class, specification)
}

open class DeclarativeSymbolProvider(
    private val identifierProvider: IdentifierProvider,
    vararg rules: DeclarativeSymbolProviderRule<out Node>
) : SymbolProvider {
    private val rules: List<DeclarativeSymbolProviderRule<out Node>> = rules.sorted()

    override fun symbolFor(node: Node?): SymbolDescription? {
        return node?.let {
            this.rules
                .firstOrNull { it.isCompatibleWith(node::class) }
                ?.invoke(identifierProvider, this, node)
        }
    }
}

class DeclarativeSymbolProviderRule<NodeTy : Node>(
    private val nodeType: KClass<NodeTy>,
    private val specification: DeclarativeSymbolProvideRuleApi<NodeTy>.(
        DeclarativeSymbolProviderRuleContext<NodeTy>
    ) -> Unit
) : DeclarativeSymbolProvideRuleApi<NodeTy>,
    (IdentifierProvider, SymbolProvider, Node) -> SymbolDescription?,
    Comparable<DeclarativeSymbolProviderRule<out Node>> {
    private var name: String? = null
    private val properties: MutableMap<String, (IdentifierProvider, Node) -> ValueDescription?> = mutableMapOf()

    override fun name(name: String) {
        this.name = name
    }

    override fun include(property: KProperty1<in NodeTy, Any?>) {
        this.properties[property.name] = { identifierProvider, node ->
            @Suppress("UNCHECKED_CAST")
            (node as? NodeTy)?.let { this.toValueDescription(identifierProvider, property.get(node)) }
        }
    }

    override fun invoke(
        identifierProvider: IdentifierProvider,
        symbolProvider: SymbolProvider,
        node: Node
    ): SymbolDescription? {
        @Suppress("UNCHECKED_CAST")
        return (node as? NodeTy)?.let {
            this.specification(DeclarativeSymbolProviderRuleContext(node, symbolProvider))
            identifierProvider.getIdentifierFor(node)?.let { identifier ->
                SymbolDescription(identifier, this.name!!, getTypes(node), getProperties(identifierProvider, node))
            }
        }
    }

    fun isCompatibleWith(nodeType: KClass<*>): Boolean {
        return this.nodeType.isSuperclassOf(nodeType)
    }

    override fun compareTo(other: DeclarativeSymbolProviderRule<out Node>): Int {
        return when {
            this.nodeType.isSuperclassOf(other.nodeType) -> 1
            other.nodeType.isSuperclassOf(this.nodeType) -> -1
            else -> (this.nodeType.qualifiedName ?: "") compareTo (other.nodeType.qualifiedName ?: "")
        }
    }

    private fun getTypes(node: NodeTy): List<String> {
        return listOfNotNull(
            node::class.qualifiedName
        ) + node::class.allSuperclasses.mapNotNull(KClass<*>::qualifiedName)
    }

    private fun getProperties(
        identifierProvider: IdentifierProvider,
        node: NodeTy
    ): Map<String, ValueDescription> {
        return this.properties.mapNotNull { (key, value) ->
            value(identifierProvider, node)?.let { valueDescription -> Pair(key, valueDescription) }
        }.toMap()
    }

    private fun toValueDescription(
        identifierProvider: IdentifierProvider,
        source: Any?
    ): ValueDescription? {
        return when (source) {
            is Boolean -> BooleanValueDescription(source)
            is Int -> IntegerValueDescription(source)
            is String -> StringValueDescription(source)
            is Node -> toContainmentValueDescription(identifierProvider, source)
            is ReferenceByName<*> -> toReferenceValueDescription(identifierProvider, source)
            is List<*> -> toListValueDescription(identifierProvider, source)
            else -> null
        }
    }

    private fun toReferenceValueDescription(
        identifierProvider: IdentifierProvider,
        source: ReferenceByName<*>
    ): ReferenceValueDescription? {
        return source.referred
            ?.let { it as? Node }
            ?.let { toReferenceValueDescription(identifierProvider, it) }
    }

    private fun toContainmentValueDescription(
        identifierProvider: IdentifierProvider,
        source: Node
    ): ContainmentValueDescription? {
        return identifierProvider.getIdentifierFor(source)?.let { ContainmentValueDescription(it) }
    }

    private fun toReferenceValueDescription(
        identifierProvider: IdentifierProvider,
        source: Node
    ): ReferenceValueDescription? {
        return identifierProvider.getIdentifierFor(source)?.let { ReferenceValueDescription(it) }
    }

    private fun toListValueDescription(
        identifierProvider: IdentifierProvider,
        source: List<*>
    ): ListValueDescription {
        return ListValueDescription(source.mapNotNull { this.toValueDescription(identifierProvider, it) }.toList())
    }
}

interface DeclarativeSymbolProvideRuleApi<NodeTy : Node> {
    fun name(name: String)

    fun include(property: KProperty1<in NodeTy, Any?>)
}

data class DeclarativeSymbolProviderRuleContext<NodeTy : Node>(val node: NodeTy, val symbolProvider: SymbolProvider)
