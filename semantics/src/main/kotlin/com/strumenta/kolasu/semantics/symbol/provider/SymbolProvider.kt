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

// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

fun symbolProvider(nodeIdProvider: NodeIdProvider, init: SymbolProviderConfigurator.() -> Unit): SymbolProvider {
    return SymbolProvider().apply { SymbolProviderConfigurator(this, nodeIdProvider).init() }
}

// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@DslMarker
annotation class SymbolProviderDsl

// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class SymbolProvider : SemanticsProvider<SymbolDescription, SymbolProviderRule<*>>()

// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@SymbolProviderDsl
class SymbolProviderConfigurator(
    symbolProvider: SymbolProvider,
    private val nodeIdProvider: NodeIdProvider
) : SemanticsProviderConfigurator<SymbolProvider, SymbolProviderRule<*>, SymbolDescription>(symbolProvider) {
    override fun <InputType : Node> createRule(nodeType: KClass<InputType>): SymbolProviderRule<*> {
        return SymbolProviderRule<InputType>(this.nodeIdProvider)
    }
}

// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@SymbolProviderDsl
class SymbolProviderRule<InputType : Node>(
    private val nodeIdProvider: NodeIdProvider
) : SemanticsProviderRule<InputType, SymbolDescription>() {

    private lateinit var name: String
    private val fields: MutableMap<String, ValueDescription> = mutableMapOf()

    @Suppress("UNUSED")
    fun include(propertyName: String, propertyValue: Any?) {
        propertyValue.toValueDescription().let { valueDescription ->
            this.fields[propertyName] = valueDescription
            if (propertyName == "name" && valueDescription is StringValueDescription) {
                this.name = valueDescription.value
            }
        }
    }

    override fun getOutput(
        input: InputType,
        provider: SemanticsProvider<SymbolDescription, *>,
        issues: MutableList<Issue>
    ) = SymbolDescription(
        name = this.getName(input),
        identifier = this.nodeIdProvider.id(input),
        type = input::class.toTypeDescription(),
        fields = this.fields
    ).withPosition(input.position)

    private fun getName(input: InputType): String {
        check(this::name.isInitialized && this.name.isNotBlank()) {
            "Rule execution error: symbol description " +
                "for ${input::class.qualifiedName} requires a non-blank name property"
        }
        return this.name
    }

    private fun KClass<*>.toTypeDescription(): TypeDescription {
        return TypeDescription(
            name = this.qualifiedName!!,
            superTypes = this.superclasses.map { it.toTypeDescription() }.toMutableList()
        )
    }

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

    private fun Boolean.toBooleanValueDescription(): BooleanValueDescription {
        return BooleanValueDescription(this)
    }

    private fun Int.toIntegerValueDescription(): IntegerValueDescription {
        return IntegerValueDescription(this)
    }

    private fun String.toStringValueDescription(): StringValueDescription {
        return StringValueDescription(this)
    }

    private fun Node.toContainmentValueDescription(): ContainmentValueDescription {
        return ContainmentValueDescription(nodeIdProvider.id(this))
    }

    private fun ReferenceByName<*>.toReferenceValueDescription(): ReferenceValueDescription {
        return ReferenceValueDescription(
            this.identifier ?: this.referred?.let { it as? Node }?.let { nodeIdProvider.id(it) }
        ).withPosition(this.position)
    }

    private fun List<*>.toListValueDescription(): ListValueDescription {
        return ListValueDescription(this.map { it.toValueDescription() }.toList())
    }
}

// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
