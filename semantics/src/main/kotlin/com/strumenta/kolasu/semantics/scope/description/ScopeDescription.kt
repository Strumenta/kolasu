package com.strumenta.kolasu.semantics.scope.description

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.semantics.scope.provider.ReferenceNode
import com.strumenta.kolasu.semantics.symbol.description.SymbolDescription

// TODO documentation

class ScopeDescription(
    private val ignoreCase: Boolean = false,
    var parentScope: ScopeDescription? = null
) {

    private val namesToExternalSymbolIdentifiers: MutableMap<String, String> = mutableMapOf()
    private val namesToLocalSymbolNodes: MutableMap<String, Node> = mutableMapOf()

    fun getSymbolNames(filter: String = ""): Sequence<String> =
        this.getLocalSymbolNames(filter)
            .plus(this.getExternalSymbolNames(filter))
            .plus(this.parentScope?.getSymbolNames(filter) ?: emptySequence())

    fun getLocalSymbolNames(filter: String = "") =
        this.namesToLocalSymbolNodes.keys.asSequence().filter { it.contains(filter) }

    fun getExternalSymbolNames(filter: String = "") =
        this.namesToExternalSymbolIdentifiers.keys.asSequence().filter { it.contains(filter) }

    fun <SymbolTy> defineLocalSymbol(symbol: SymbolTy) where SymbolTy : Node, SymbolTy : PossiblyNamed {
        check(symbol.name != null) {
            "Error while adding local symbol in scope description - name is null"
        }
        this.defineLocalSymbol(symbol.name!!, symbol)
    }

    fun defineLocalSymbol(name: String, symbol: Node) {
        check(name.isNotBlank()) {
            "Error while adding local symbol in scope description - name is blank"
        }
        this.namesToLocalSymbolNodes[name.asKey()] = symbol
    }

    fun defineExternalSymbol(symbol: SymbolDescription) {
        this.defineExternalSymbol(symbol.name, symbol)
    }

    fun defineExternalSymbol(name: String, symbol: SymbolDescription) {
        this.defineExternalSymbol(name, symbol.identifier)
    }

    fun defineExternalSymbol(name: String, identifier: String) {
        check(name.isNotBlank()) {
            "Error while adding external symbol in scope description - name is blank or null"
        }
        check(identifier.isNotBlank()) {
            "Error while adding external symbol in scope description - identifier is blank or null"
        }
        this.namesToExternalSymbolIdentifiers[name.asKey()] = identifier
    }

    fun resolve(node: ReferenceNode<*>) {
        val name = node.reference.name.asKey()
        val localSymbolNode by lazy { this.namesToLocalSymbolNodes[name] as? PossiblyNamed }
        val externalSymbolIdentifier by lazy { this.namesToExternalSymbolIdentifiers[name] }
        when {
            localSymbolNode != null -> {
                @Suppress("UNCHECKED_CAST")
                (node.reference as ReferenceByName<PossiblyNamed>).referred = localSymbolNode
            }
            externalSymbolIdentifier != null -> {
                node.reference.identifier = externalSymbolIdentifier
            }
            else -> {
                this.parentScope?.resolve(node)
            }
        }
    }

    private fun String.asKey(): String {
        return if (ignoreCase) this.lowercase() else this
    }
}
