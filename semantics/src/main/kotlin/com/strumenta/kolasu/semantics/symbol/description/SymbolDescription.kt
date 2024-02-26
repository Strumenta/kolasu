package com.strumenta.kolasu.semantics.symbol.description

data class SymbolDescription(
    val identifier: String,
    val types: List<String>,
    val properties: Map<String, ValueDescription>
)

sealed interface ValueDescription {
    val value: Any?
}

data class BooleanValueDescription(
    override val value: Boolean? = null
) : ValueDescription

data class IntegerValueDescription(
    override val value: Int? = null
) : ValueDescription

data class StringValueDescription(
    override val value: String? = null
) : ValueDescription

data class ReferenceValueDescription(
    override val value: SymbolDescription? = null
) : ValueDescription

data class ContainmentValueDescription(
    override val value: SymbolDescription? = null
) : ValueDescription

data class ListValueDescription(
    override val value: List<ValueDescription> = emptyList()
) : ValueDescription
