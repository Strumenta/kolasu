package com.strumenta.kolasu.semantics.symbol.provider

import com.strumenta.kolasu.ids.NodeIdProvider
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.withPosition
import com.strumenta.kolasu.semantics.provider.SemanticsProvider
import com.strumenta.kolasu.semantics.provider.SemanticsProviderConfigurator
import com.strumenta.kolasu.semantics.provider.SemanticsProviderRule
import com.strumenta.kolasu.semantics.symbol.description.BooleanValueDescription
import com.strumenta.kolasu.semantics.symbol.description.ContainmentValueDescription
import com.strumenta.kolasu.semantics.symbol.description.IntegerValueDescription
import com.strumenta.kolasu.semantics.symbol.description.ListValueDescription
import com.strumenta.kolasu.semantics.symbol.description.NullValueDescription
import com.strumenta.kolasu.semantics.symbol.description.ReferenceValueDescription
import com.strumenta.kolasu.semantics.symbol.description.StringValueDescription
import com.strumenta.kolasu.semantics.symbol.description.SymbolDescription
import com.strumenta.kolasu.semantics.symbol.description.TypeDescription
import com.strumenta.kolasu.semantics.symbol.description.ValueDescription
import com.strumenta.kolasu.validation.Issue
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

/**
 * Type-safe builder to create new [SymbolProvider] instances.
 *
 * @param nodeIdProvider [NodeIdProvider] instance used to associate nodes to identifiers
 * @param init configuration of the symbol provider
 * @return a [SymbolProvider] instance realising the specified rules
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
fun symbolProvider(nodeIdProvider: NodeIdProvider, init: SymbolProviderConfigurator.() -> Unit): SymbolProvider {
    return SymbolProvider().apply { SymbolProviderConfigurator(this, nodeIdProvider).init() }
}

/**
 * Annotation class grouping elements of the Symbol Provider
 * DSL - used to configure [SymbolProvider] instances.
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
@DslMarker
annotation class SymbolProviderDsl

/**
 * Query-side representation of a [SymbolProvider] associating
 * [Node] instances to the corresponding [SymbolDescription].
 *
 * @see SemanticsProvider
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
class SymbolProvider : SemanticsProvider<SymbolDescription, SymbolProviderRule<*>>()

/**
 * Configuration-side representation of a [SymbolProvider]
 * supporting the declarative specification of symbol provision rules.
 *
 * @param symbolProvider the configured [SymbolProvider] instance
 * @param nodeIdProvider [NodeIdProvider] instance used to associate nodes to identifiers
 * @see SemanticsProviderConfigurator
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
@SymbolProviderDsl
class SymbolProviderConfigurator(
    symbolProvider: SymbolProvider,
    private val nodeIdProvider: NodeIdProvider
) : SemanticsProviderConfigurator<SymbolProvider, SymbolProviderRule<*>, SymbolDescription>(symbolProvider) {
    override fun <NodeType : Node> createRule(nodeType: KClass<NodeType>): SymbolProviderRule<*> {
        return SymbolProviderRule<NodeType>(this.nodeIdProvider)
    }
}

/**
 * Symbol provider rule definition exposing the configuration API
 * for single symbol provision rules and handling the actual evaluation.
 *
 * @param nodeIdProvider [NodeIdProvider] instance used to associate nodes to identifiers
 * @see SemanticsProviderRule
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
@SymbolProviderDsl
class SymbolProviderRule<NodeTy : Node>(
    private val nodeIdProvider: NodeIdProvider
) : SemanticsProviderRule<NodeTy, SymbolDescription>() {

    /**
     * Name of the output symbol description.
     **/
    private lateinit var name: String

    /**
     * Fields of the output symbol description.
     **/
    private val fields: MutableMap<String, ValueDescription> = mutableMapOf()

    /**
     * Include a property with the given [propertyName] and
     * [propertyValue] in the symbol description.
     * @param propertyName the property name
     * @param propertyValue the property value
     **/
    fun include(propertyName: String, propertyValue: Any?) {
        propertyValue.toValueDescription().let { valueDescription ->
            this.fields[propertyName] = valueDescription
            if (propertyName == "name" && valueDescription is StringValueDescription) {
                this.name = valueDescription.value
            }
        }
    }

    override fun getOutput(
        node: NodeTy,
        provider: SemanticsProvider<SymbolDescription, *>,
        issues: MutableList<Issue>
    ) = SymbolDescription(
        name = this.getName(node),
        identifier = this.nodeIdProvider.id(node),
        type = node::class.toTypeDescription(),
        fields = this.fields
    ).withPosition(node.position)

    /**
     * Retrieve the name of the output symbol description, throw error if unset.
     * @param node the rule input node
     * @return the name of the output symbol description, throws error if unset.
     **/
    private fun getName(node: NodeTy): String {
        check(this::name.isInitialized && this.name.isNotBlank()) {
            "Rule execution error: symbol description " +
                "for ${node::class.qualifiedName} requires a non-blank name property"
        }
        return this.name
    }

    /**
     * Extract a [TypeDescription] for the output symbol description.
     * @return a [TypeDescription] for the output symbol description
     **/
    private fun KClass<*>.toTypeDescription(): TypeDescription {
        return TypeDescription(
            name = this.qualifiedName!!,
            superTypes = this.superclasses.map { it.toTypeDescription() }.toMutableList()
        )
    }

    /**
     * Extension method to transform an [Any] value into a [ValueDescription].
     * @return the corresponding [ValueDescription]
     **/
    private fun Any?.toValueDescription(): ValueDescription {
        return when (this) {
            is Boolean -> this.toBooleanValueDescription()
            is Int -> this.toIntegerValueDescription()
            is String -> this.toStringValueDescription()
            is Node -> this.toContainmentValueDescription()
            is ReferenceByName<*> -> this.toReferenceValueDescription()
            is List<*> -> this.toListValueDescription()
            null -> NullValueDescription
            else -> throw RuntimeException(
                "Rule execution error:" +
                    " unsupported value description for ${this::class.qualifiedName}"
            )
        }
    }

    /**
     * Transform a [Boolean] value into a [BooleanValueDescription]
     * @return a [BooleanValueDescription] for the given [Boolean] value
     **/
    private fun Boolean.toBooleanValueDescription(): BooleanValueDescription {
        return BooleanValueDescription(this)
    }

    /**
     * Transforms an [Int] value into an [IntegerValueDescription]
     * @return an [IntegerValueDescription] for the given [Int]
     **/
    private fun Int.toIntegerValueDescription(): IntegerValueDescription {
        return IntegerValueDescription(this)
    }

    /**
     * Transforms a [String] value into a [StringValueDescription].
     * @return a [StringValueDescription] for the given [String]
     **/
    private fun String.toStringValueDescription(): StringValueDescription {
        return StringValueDescription(this)
    }

    /**
     * Transforms a [Node] value into a [ContainmentValueDescription].
     * @return a [ContainmentValueDescription] for the given [Node]
     **/
    private fun Node.toContainmentValueDescription(): ContainmentValueDescription {
        return ContainmentValueDescription(nodeIdProvider.id(this))
    }

    /**
     * Transforms a [ReferenceByName] value into a [ReferenceValueDescription].
     * @return a [ReferenceValueDescription] for the given [ReferenceByName]
     **/
    private fun ReferenceByName<*>.toReferenceValueDescription(): ReferenceValueDescription {
        return ReferenceValueDescription(
            this.identifier ?: this.referred?.let { it as? Node }?.let { nodeIdProvider.id(it) }
        ).withPosition(this.position)
    }

    /**
     * Transforms a [List] value into a [ListValueDescription].
     * @return a [ListValueDescription] for the given [List]
     **/
    private fun List<*>.toListValueDescription(): ListValueDescription {
        return ListValueDescription(this.map { it.toValueDescription() }.toList())
    }
}
