package com.strumenta.kolasu.symbolresolution

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.nodeProperties
import com.strumenta.kolasu.utils.memoize
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf

// TODO handle case-sensitivity : boolean
// TODO handle multiple symbols (e.g. function overloading)
// TODO allow other than name-based symbol binding (e.g. predicated, numbered, etc.)
data class Scope(var parent: Scope? = null, var symbolTable: SymbolTable = mutableMapOf()) {
    fun define(symbol: Symbol) {
        val name: String = symbol.name ?: throw IllegalArgumentException("The given symbol must have a name")
        this.symbolTable.computeIfAbsent(name) { mutableListOf() }.add(symbol)
    }

    fun resolve(name: String, type: KClass<out Symbol> = Symbol::class): Symbol? {
        return this.symbolTable.getOrDefault(name, mutableListOf()).find { type.isInstance(it) }
            ?: this.parent?.resolve(name, type)
    }
}

class ScopeDefinition(val contextType: KClass<out Node>, scopeFunction: (Node) -> Scope?) {
    val scopeFunction: (Node) -> Scope? = scopeFunction.memoize()
}

typealias ClassScopeDefinitions = MutableMap<KClass<*>, MutableList<ScopeDefinition>>
typealias PropertyScopeDefinitions = MutableMap<ReferenceByNameProperty, MutableList<ScopeDefinition>>

// ReferenceByNameProperty

typealias ReferenceByNameProperty = KProperty1<out Node, ReferenceByName<out Symbol>?>

@Suppress("unchecked_cast")
fun ReferenceByNameProperty.getReferredType(): KClass<out Symbol> {
    return this.returnType.arguments[0].type!!.classifier!! as KClass<out Symbol>
}

@Suppress("unchecked_cast")
fun Node.referenceByNameProperties(): Collection<ReferenceByNameProperty> {
    return this.nodeProperties
        .filter {
            it.returnType
                .isSubtypeOf(
                    ReferenceByName::class.createType(
                        arguments = listOf(
                            KTypeProjection(variance = KVariance.OUT, type = Symbol::class.createType())
                        ),
                        nullable = true
                    )
                )
        }.map { it as ReferenceByNameProperty }
}
