package com.strumenta.kolasu.semantics.symbol.provider.declarative

import com.strumenta.kolasu.ids.NodeIdProvider
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.semantics.symbol.description.BooleanValueDescription
import com.strumenta.kolasu.semantics.symbol.description.ContainmentValueDescription
import com.strumenta.kolasu.semantics.symbol.description.IntegerValueDescription
import com.strumenta.kolasu.semantics.symbol.description.ListValueDescription
import com.strumenta.kolasu.semantics.symbol.description.NullValueDescription
import com.strumenta.kolasu.semantics.symbol.description.ReferenceValueDescription
import com.strumenta.kolasu.semantics.symbol.description.StringValueDescription
import com.strumenta.kolasu.semantics.symbol.description.SymbolDescription
import com.strumenta.kolasu.semantics.symbol.description.ValueDescription
import com.strumenta.kolasu.semantics.symbol.provider.SymbolProvider
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.safeCast

inline fun <reified NodeTy : NodeLike> symbolFor(
    noinline specification: DeclarativeSymbolProvideRuleApi<NodeTy>.(
        DeclarativeSymbolProviderRuleContext<NodeTy>,
    ) -> Unit,
): DeclarativeSymbolProviderRule<NodeTy> {
    return DeclarativeSymbolProviderRule(NodeTy::class, specification)
}

open class DeclarativeSymbolProvider(
    private val nodeIdProvider: NodeIdProvider,
    vararg rules: DeclarativeSymbolProviderRule<out NodeLike>,
) : SymbolProvider {
    private val rules: List<DeclarativeSymbolProviderRule<out NodeLike>> = rules.sorted()

    override fun symbolFor(node: NodeLike): SymbolDescription? {
        return this
            .rules
            .firstOrNull { it.isCompatibleWith(node::class) }
            ?.invoke(nodeIdProvider, this, node)
    }
}

class DeclarativeSymbolProviderRule<NodeTy : NodeLike>(
    private val nodeType: KClass<NodeTy>,
    private val specification: DeclarativeSymbolProvideRuleApi<NodeTy>.(
        DeclarativeSymbolProviderRuleContext<NodeTy>,
    ) -> Unit,
) : DeclarativeSymbolProvideRuleApi<NodeTy>,
    (NodeIdProvider, SymbolProvider, NodeLike) -> SymbolDescription,
    Comparable<DeclarativeSymbolProviderRule<out NodeLike>> {
    private var name: (NodeLike) -> String? = { null }
    private val properties: MutableMap<String, (NodeIdProvider, NodeLike) -> ValueDescription> = mutableMapOf()

    override fun name(name: String) {
        this.name = { name }
    }

    override fun include(property: KProperty1<in NodeTy, Any?>) {
        if (property.name == "name") {
            this.name = { node ->
                @Suppress("UNCHECKED_CAST")
                String::class.safeCast(property.get(node as NodeTy))
            }
        }
        this.properties[property.name] = { nodeIdProvider, node ->
            @Suppress("UNCHECKED_CAST")
            this.toValueDescription(nodeIdProvider, property.get(node as NodeTy))
        }
    }

    override fun invoke(
        nodeIdProvider: NodeIdProvider,
        symbolProvider: SymbolProvider,
        node: NodeLike,
    ): SymbolDescription {
        @Suppress("UNCHECKED_CAST")
        return (node as NodeTy).let {
            this.specification(DeclarativeSymbolProviderRuleContext(node, symbolProvider))
            SymbolDescription(
                getName(node),
                nodeIdProvider.id(node),
                getTypes(node),
                getProperties(nodeIdProvider, node),
            )
        }
    }

    fun isCompatibleWith(nodeType: KClass<*>): Boolean = this.nodeType.isSuperclassOf(nodeType)

    override fun compareTo(other: DeclarativeSymbolProviderRule<out NodeLike>): Int =
        when {
            this.nodeType.isSuperclassOf(other.nodeType) -> 1
            other.nodeType.isSuperclassOf(this.nodeType) -> -1
            else -> (this.nodeType.qualifiedName ?: "") compareTo (other.nodeType.qualifiedName ?: "")
        }

    private fun getName(node: NodeTy): String =
        this.name(node)
            ?: throw RuntimeException("Symbol description name property not set for node: ${node::class.qualifiedName}")

    private fun getTypes(node: NodeTy): List<String> =
        listOfNotNull(
            node::class.qualifiedName,
        ) + node::class.allSuperclasses.mapNotNull(KClass<*>::qualifiedName)

    private fun getProperties(
        nodeIdProvider: NodeIdProvider,
        node: NodeTy,
    ): Map<String, ValueDescription> =
        this.properties.mapValues { (_, valueDescriptionProvider) ->
            valueDescriptionProvider(nodeIdProvider, node)
        }

    private fun toValueDescription(
        nodeIdProvider: NodeIdProvider,
        source: Any?,
    ): ValueDescription =
        when (source) {
            is Boolean -> BooleanValueDescription(source)
            is Int -> IntegerValueDescription(source)
            is String -> StringValueDescription(source)
            is NodeLike -> toContainmentValueDescription(nodeIdProvider, source)
            is ReferenceByName<*> -> toReferenceValueDescription(nodeIdProvider, source)
            is List<*> -> toListValueDescription(nodeIdProvider, source)
            null -> NullValueDescription
            else -> throw RuntimeException("Unsupported value description for ${source::class.qualifiedName}")
        }

    private fun toReferenceValueDescription(
        nodeIdProvider: NodeIdProvider,
        source: ReferenceByName<*>,
    ): ReferenceValueDescription =
        ReferenceValueDescription(
            source.referred?.let { it as? NodeLike }?.let { nodeIdProvider.id(it) },
        )

    private fun toContainmentValueDescription(
        nodeIdProvider: NodeIdProvider,
        source: NodeLike,
    ): ContainmentValueDescription = ContainmentValueDescription(nodeIdProvider.id(source))

    private fun toListValueDescription(
        nodeIdProvider: NodeIdProvider,
        source: List<*>,
    ): ListValueDescription = ListValueDescription(source.map { this.toValueDescription(nodeIdProvider, it) }.toList())
}

interface DeclarativeSymbolProvideRuleApi<NodeTy : NodeLike> {
    fun name(name: String)

    fun include(property: KProperty1<in NodeTy, Any?>)
}

data class DeclarativeSymbolProviderRuleContext<NodeTy : NodeLike>(
    val node: NodeTy,
    val symbolProvider: SymbolProvider,
)
