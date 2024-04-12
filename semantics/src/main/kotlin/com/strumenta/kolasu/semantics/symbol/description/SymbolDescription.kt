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
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
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
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
data class TypeDescription(
    override val name: String,
    val superTypes: MutableList<TypeDescription> = mutableListOf()
) : Node(), Named {

    /**
     * Checks whether the described type is a subtype of the given [type].
     *
     * @param type the possible super-type
     * @return true if [type] is a super-type, false otherwise
     **/
    fun isSubTypeOf(type: KClass<*>?): Boolean {
        return this.name == type?.qualifiedName ||
            this.superTypes.any { it.isSubTypeOf(type) }
    }

    /**
     * Checks whether the described type is a subtype of the given [type].
     *
     * @param type the possible super-type
     * @return true if [type] is a super-type, false otherwise
     **/
    fun isSubTypeOf(type: TypeDescription?): Boolean {
        return this.name == type?.name ||
            this.superTypes.any { it.isSubTypeOf(type) }
    }

    /**
     * Checks whether the described type is a supertype of the given [type].
     *
     * @param type the possible subtype
     * @return true if [type] is a subtype, false otherwise
     **/
    fun isSuperTypeOf(type: KClass<*>?): Boolean {
        return this.name == type?.qualifiedName ||
            type?.allSuperclasses?.any { this.isSuperTypeOf(it) } ?: false
    }

    /**
     * Checks whether the described type is a supertype of the given [type].
     *
     * @param type the possible subtype
     * @return true if [type] is a subtype, false otherwise
     **/
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
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
sealed class ValueDescription(open val value: Any? = null) : Node()

/**
 * Description for boolean values.
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
data class BooleanValueDescription(
    override val value: Boolean
) : ValueDescription(value)

/**
 * Description for integer values.
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
data class IntegerValueDescription(
    override val value: Int
) : ValueDescription(value)

/**
 * Description for string values.
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
data class StringValueDescription(
    override val value: String
) : ValueDescription(value)

/**
 * Description for null values.
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
object NullValueDescription : ValueDescription(null) {
    private fun readResolve(): Any = NullValueDescription
}

/**
 * Description for [ReferenceByName] values - the [value]
 * property represents the reference target node identifier.
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
data class ReferenceValueDescription(
    override val value: String? = null
) : ValueDescription(value)

/**
 * Description for children [Node] values - the [value]
 * property represents the contained node identifier.
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
data class ContainmentValueDescription(
    override val value: String
) : ValueDescription(value)

/**
 * Description for [List] values.
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
data class ListValueDescription(
    override val value: List<ValueDescription> = emptyList()
) : ValueDescription(value)

/**
 * Language identifier for the Kolasu Symbol Description Language
 **/
const val KOLASU_SYMBOL_DESCRIPTION_LANGUAGE_NAME = "com.strumenta.kolasu.semantics.symbol.SymbolDescriptionLanguage"

/**
 * Language definition for the Kolasu Symbol Description Language
 **/
val KOLASU_SYMBOL_DESCRIPTION_LANGUAGE: KolasuLanguage by lazy {
    KolasuLanguage(KOLASU_SYMBOL_DESCRIPTION_LANGUAGE_NAME).apply {
        addClass(SymbolDescription::class)
    }
}
