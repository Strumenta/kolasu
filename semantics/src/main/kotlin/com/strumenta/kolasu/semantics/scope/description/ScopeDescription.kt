package com.strumenta.kolasu.semantics.scope.description

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.semantics.symbol.description.SymbolDescription
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf

/**
 * Description of the visible names at a given point in time-space.
 *
 * @param ignoreCase flag indicating that name resolution should be case-insensitive (if true)
 * @param parent the parent [ScopeDescription] containing this scope description
 *
 * @author Lorenzo Addazi <lorenzo.addazi@strumenta.com>
 **/
class ScopeDescription(
    var ignoreCase: Boolean = false,
    var parent: ScopeDescription? = null
) {

    /**
     * Map associating symbol names with case-insensitive keys - used to keep
     * case information about symbols (might change in keys when [ignoreCase] is true).
     **/
    private val namesToSymbolKeys: MutableMap<String, String> = mutableMapOf()

    /**
     * Map associating symbol keys to external symbol definitions - [SymbolDescription] instances
     **/
    private val symbolKeysToExternalDefinitions: MutableMap<String, MutableList<SymbolDescription>> = mutableMapOf()

    /**
     * Map associating symbol keys to local symbol definitions - [Node] instances
     **/
    private val symbolKeysToLocalDefinitions: MutableMap<String, MutableList<Node>> = mutableMapOf()

    /**
     * Retrieve all names defines in this scope description.
     * @param predicate optional filter over the names
     * @return the names in this scope description (optionally filtered using [predicate])
     **/
    fun names(predicate: (String) -> Boolean = { true }): List<String> {
        return this.namesToSymbolKeys.keys.filter(predicate)
            .plus(this.parent?.names(predicate) ?: emptyList())
    }

    /**
     * Include the given [name]-[symbol] association in the scope description.
     * @param symbol the symbol to include
     * @param name the name of the symbol (possibly inferred from [symbol])
     **/
    fun include(symbol: Any?, name: String? = null) {
        require(symbol != null) {
            "Error while including symbol in scope description: symbol cannot be null."
        }
        require(symbol is SymbolDescription || symbol is Node) {
            "Error while including symbol in scope description: " +
                "expected Node or SymbolDescription, received ${symbol::class.qualifiedName}."
        }
        val symbolName = name ?: symbol.let { it as? PossiblyNamed }?.name
        require(!symbolName.isNullOrBlank()) {
            "Error while including symbol in scope description: name cannot be blank or null."
        }
        val symbolKey = symbolName.asKey()
        this.namesToSymbolKeys[symbolName] = symbolKey
        when (symbol) {
            is SymbolDescription -> this.symbolKeysToExternalDefinitions.getOrPut(symbolKey) { mutableListOf() }.add(
                symbol
            )
            is Node -> this.symbolKeysToLocalDefinitions.getOrPut(symbolKey) { mutableListOf() }.add(symbol)
        }
    }

    /**
     * Resolve the given [reference] using this scope description, possibly narrowing the candidates with [type].
     * @param reference the reference to resolve
     * @param type the target type of the reference to resolve
     **/
    fun resolve(reference: ReferenceByName<*>?, type: KClass<out PossiblyNamed> = PossiblyNamed::class) {
        reference?.let {
            when (val symbol = this.findSymbol(reference.name.asKey(), type)) {
                is SymbolDescription -> reference.identifier = symbol.identifier
                is PossiblyNamed -> (reference as ReferenceByName<PossiblyNamed>).referred = symbol
                else -> this.parent?.resolve(reference, type)
            }
        }
    }

    /**
     * Retrieve the first symbol with the given [name] and [type].
     * @param name the name of the symbol
     * @param type the type of the symbol
     * @return the corresponding symbol (if any) - precedence is given to local symbol definitions
     **/
    private fun findSymbol(name: String, type: KClass<out PossiblyNamed>): Any? {
        val localSymbol = this.findLocalSymbol(name, type)
        val externalSymbol = this.findExternalSymbol(name, type)
        return when {
            localSymbol == null -> externalSymbol
            externalSymbol == null -> localSymbol
            else -> localSymbol.takeIf { externalSymbol.type.isSuperTypeOf(localSymbol::class) } ?: externalSymbol
        }
    }

    /**
     * Retrieve the first local symbol with the given [name] and [type].
     * @param name the name of the symbol
     * @param type the type of the symbol
     * @return the corresponding local symbol (if any)
     **/
    private fun findLocalSymbol(name: String, type: KClass<out PossiblyNamed>): Node? {
        return this.symbolKeysToLocalDefinitions[name]
            ?.let { it.filter { localSymbol -> localSymbol::class.isSubclassOf(type) } }
            ?.sortedWith { left, right ->
                when {
                    left::class.isSuperclassOf(right::class) -> 1
                    left::class.isSubclassOf(right::class) -> -1
                    else -> (left::class.qualifiedName ?: "") compareTo (right::class.qualifiedName ?: "")
                }
            }?.firstOrNull()
    }

    /**
     * Retrieve the first external symbol with the given [name] and [type].
     * @param name the name of the symbol
     * @param type the type of the symbol
     * @return the corresponding external symbol (if any)
     **/
    private fun findExternalSymbol(name: String, type: KClass<out PossiblyNamed>): SymbolDescription ? {
        return this.symbolKeysToExternalDefinitions[name]
            ?.let { it.filter { externalSymbol -> externalSymbol.type.isSubTypeOf(type) } }
            ?.sortedWith { left, right ->
                when {
                    left.type.isSuperTypeOf(right.type) -> 1
                    left.type.isSubTypeOf(right.type) -> -1
                    else -> left.type.name compareTo right.type.name
                }
            }?.firstOrNull()
    }

    /**
     * Converts a symbol name into a symbol key (possibly considering casing)
     * @return the symbol key for the given name
     **/
    private fun String.asKey(): String {
        return if (ignoreCase) this.lowercase() else this
    }
}
