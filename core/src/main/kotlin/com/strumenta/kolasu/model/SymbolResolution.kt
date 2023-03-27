package com.strumenta.kolasu.model

import kotlin.reflect.KClass

interface Symbol : PossiblyNamed

/**
 * A scope containing symbols
 **/
data class Scope(
    var parent: Scope? = null,
    val symbols: MutableMap<String, MutableList<Symbol>> = mutableMapOf(),
) {
    fun define(symbol: Symbol) {
        val name: String = symbol.name ?: throw IllegalArgumentException("The given symbol must have a name")
        this.symbols.computeIfAbsent(name) { mutableListOf() }.add(symbol)
    }

    fun resolve(name: String, type: KClass<*> = Symbol::class): Symbol? {
        return this.symbols.getOrDefault(name, mutableListOf()).find { type.isInstance(it) }
            ?: this.parent?.resolve(name, type)
    }
}

/**
 * A node that defines a symbol
 **/
interface SymbolProvider {
    val symbols: List<Symbol>
}

// fun Node.resolveSymbols() {
//    val referenceByNames = this.properties
//        .mapNotNull { property -> if (property.value is ReferenceByName<*>) property.value else null }
//    if (referenceByNames.isNotEmpty()) {
//        val activeScope: Scope? =
//            if (this is DeclarativeScopeProvider) {
//                this.scope
//            } else {
//                this.findAncestorOfType(DeclarativeScopeProvider::class.java)?.scope
//            }
//        referenceByNames.forEach {
//            tryCast<ReferenceByName<Symbol>>(it) {
//                this.tryToResolve(activeScope?.resolve(it.name))
//                // scopeProvider.scopeFor(this, property) -> List<Node>
//                // kClass della property
//                // VariableAssignment::variable
//                // scopeFor_VariableAssignment_variable(context: N < Variable)
//                // this.tryToResolve()
//
//                // NamedElement::name
//                // scopeFor_NamedElement_name(NamedElement)
//                // VariableAssignment::variable -> VariableSymbol
//                //
//            }
//        }
//    }
//    this.walkChildren().forEach { it.resolveSymbols() }
// }

inline fun <reified T> tryCast(instance: Any?, block: T.() -> Unit) {
    if (instance is T) { block(instance) }
}
