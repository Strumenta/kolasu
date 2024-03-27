package com.strumenta.kolasu.semantics.symbol.description

import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses

/**
 * The description of a [Node] from the outside,
 * i.e. from other trees not containing the node.
 * @property name the name of the symbol
 * @property identifier the identifier of the described node
 * @property type a description of the type of the described node
 * @property fields the visible fields of the described node
 **/
data class SymbolDescription(
    override var name: String,
    var identifier: String,
    val type: TypeDescription,
    val fields: Map<String, ValueDescription>
) : Node(), Named

/**
 * The type description of a symbol.
 * @property name the name of the type
 * @property superTypes possible super types
 **/
data class TypeDescription(
    override val name: String,
    val superTypes: MutableList<TypeDescription> = mutableListOf()
) : Node(), Named {

    fun isSubTypeOf(type: KClass<*>?): Boolean {
        return this.name == type?.qualifiedName ||
            this.superTypes.any { it.isSubTypeOf(type) }
    }

    fun isSubTypeOf(type: TypeDescription?): Boolean {
        return this.name == type?.name ||
            this.superTypes.any { it.isSubTypeOf(type) }
    }

    fun isSuperTypeOf(type: KClass<*>?): Boolean {
        return this.name == type?.qualifiedName ||
            type?.allSuperclasses?.any { this.isSuperTypeOf(it) } ?: false
    }

    fun isSuperTypeOf(type: TypeDescription?): Boolean {
        return this.name == type?.name ||
            type?.superTypes?.any { this.isSuperTypeOf(it) } ?: false
    }
}

/**
 * The value of fields in [SymbolDescription] instances.
 * Supported types:
 * - [Boolean] as [BooleanValueDescription]
 * - [Integer] as [IntegerValueDescription]
 * - [String] as [StringValueDescription]
 * - [Nothing] as [NullValueDescription]
 * - [ReferenceByName] : [ReferenceValueDescription]
 * - [Node] as [ContainmentValueDescription]
 * - [List] as [ListValueDescription]
 * @param value the actual value
 **/
sealed class ValueDescription(open val value: Any? = null) : Node()

data class BooleanValueDescription(
    override val value: Boolean
) : ValueDescription(value)

data class IntegerValueDescription(
    override val value: Int
) : ValueDescription(value)

data class StringValueDescription(
    override val value: String
) : ValueDescription(value)

object NullValueDescription : ValueDescription(null) {
    private fun readResolve(): Any = NullValueDescription
}

data class ReferenceValueDescription(
    override val value: String? = null
) : ValueDescription(value)

data class ContainmentValueDescription(
    override val value: String
) : ValueDescription(value)

data class ListValueDescription(
    override val value: List<ValueDescription> = emptyList()
) : ValueDescription(value)

const val KOLASU_SYMBOL_DESCRIPTION_LANGUAGE_NAME = "com.strumenta.kolasu.semantics.symbol.SymbolDescriptionLanguage"

val KOLASU_SYMBOL_DESCRIPTION_LANGUAGE: KolasuLanguage by lazy {
    KolasuLanguage(KOLASU_SYMBOL_DESCRIPTION_LANGUAGE_NAME).apply {
        addClass(SymbolDescription::class)
    }
}
