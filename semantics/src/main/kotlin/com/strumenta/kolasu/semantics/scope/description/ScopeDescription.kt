package com.strumenta.kolasu.semantics.scope.description

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.semantics.symbol.description.SymbolDescription
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf

class ScopeDescription(
    var ignoreCase: Boolean = false,
    var parent: ScopeDescription? = null
) {

    private val namesToExternalSymbols: MutableMap<String, MutableList<SymbolDescription>> = mutableMapOf()
    private val namesToLocalSymbols: MutableMap<String, MutableList<PossiblyNamed>> = mutableMapOf()

    fun names(filter: String = ""): List<String> {
        return this.namesToLocalSymbols.keys.filter { it.contains(filter) }
            .plus(this.namesToExternalSymbols.keys.filter { it.contains(filter) })
            .plus(this.parent?.names(filter) ?: emptyList())
    }

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
        when (symbol) {
            is SymbolDescription ->
                this.namesToExternalSymbols.getOrPut(symbolName.asKey()) { mutableListOf() }.add(symbol)
            is PossiblyNamed ->
                this.namesToLocalSymbols.getOrPut(symbolName.asKey()) { mutableListOf() }.add(symbol)
        }
    }

    fun resolve(reference: ReferenceByName<*>?, type: KClass<out PossiblyNamed> = PossiblyNamed::class) {
        reference?.let {
            when (val symbol = this.findSymbol(reference.name.asKey(), type)) {
                is SymbolDescription -> reference.identifier = symbol.identifier
                is PossiblyNamed -> (reference as ReferenceByName<PossiblyNamed>).referred = symbol
                else -> this.parent?.resolve(reference, type)
            }
        }
    }

    private fun findSymbol(name: String, type: KClass<out PossiblyNamed>): Any? {
        val localSymbol = this.findLocalSymbol(name, type)
        val externalSymbol = this.findExternalSymbol(name, type)
        return when {
            localSymbol == null -> externalSymbol
            externalSymbol == null -> localSymbol
            else -> localSymbol.takeIf { externalSymbol.type.isSuperTypeOf(localSymbol::class) } ?: externalSymbol
        }
    }

    private fun findLocalSymbol(name: String, type: KClass<out PossiblyNamed>): PossiblyNamed? {
        return this.namesToLocalSymbols[name]
            ?.let { it.filter { localSymbol -> localSymbol::class.isSubclassOf(type) } }
            ?.sortedWith { left, right ->
                when {
                    left::class.isSuperclassOf(right::class) -> 1
                    left::class.isSubclassOf(right::class) -> -1
                    else -> (left::class.qualifiedName ?: "") compareTo (right::class.qualifiedName ?: "")
                }
            }?.firstOrNull()
    }

    private fun findExternalSymbol(name: String, type: KClass<out PossiblyNamed>): SymbolDescription ? {
        return this.namesToExternalSymbols[name]
            ?.let { it.filter { externalSymbol -> externalSymbol.type.isSubTypeOf(type) } }
            ?.sortedWith { left, right ->
                when {
                    left.type.isSuperTypeOf(right.type) -> 1
                    left.type.isSubTypeOf(right.type) -> -1
                    else -> left.type.name compareTo right.type.name
                }
            }?.firstOrNull()
    }

    private fun String.asKey(): String {
        return if (ignoreCase) this.lowercase() else this
    }
}
