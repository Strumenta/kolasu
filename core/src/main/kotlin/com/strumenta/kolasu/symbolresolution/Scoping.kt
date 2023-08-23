package com.strumenta.kolasu.symbolresolution

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.nodeProperties
import com.strumenta.kolasu.utils.memoize
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf

// TODO handle multiple symbols (e.g. function overloading)
// TODO allow other than name-based symbol binding (e.g. predicated, numbered, etc.)
data class Scope(
    var parent: Scope? = null,
    val symbolTable: SymbolTable = mutableMapOf(),
    val ignoreCase: Boolean = false
) {
    fun define(symbol: PossiblyNamed) {
        this.symbolTable.computeIfAbsent(symbol.name.toSymbolTableKey()) { mutableListOf() }.add(symbol)
    }

    fun resolve(name: String, type: KClass<out PossiblyNamed> = PossiblyNamed::class): PossiblyNamed? {
        val key = name.toSymbolTableKey()
        return this.symbolTable.getOrDefault(key, mutableListOf()).find { type.isInstance(it) }
            ?: this.parent?.resolve(key, type)
    }

    private fun String?.toSymbolTableKey() = when {
        this != null && ignoreCase -> this.lowercase()
        this != null -> this
        else -> throw IllegalArgumentException("The given symbol must have a name")
    }
}

typealias SymbolTable = MutableMap<String, MutableList<PossiblyNamed>>

class ScopeDefinition(val contextType: KClass<out Node>, scopeFunction: (Node) -> Scope?) {
    val scopeFunction: (Node) -> Scope? = scopeFunction.memoize()
}

typealias ClassScopeDefinitions = MutableMap<KClass<*>, MutableList<ScopeDefinition>>
typealias PropertyScopeDefinitions = MutableMap<ReferenceByNameProperty, MutableList<ScopeDefinition>>

// ReferenceByNameProperty

typealias ReferenceByNameProperty = KProperty1<out Node, ReferenceByName<out PossiblyNamed>?>

@Suppress("unchecked_cast")
fun ReferenceByNameProperty.getReferredType(): KClass<out PossiblyNamed> {
    return this.returnType.arguments[0].type!!.classifier!! as KClass<out PossiblyNamed>
}

@Suppress("unchecked_cast")
fun Node.referenceByNameProperties(): Collection<ReferenceByNameProperty> {
    return this.nodeProperties
        .filter {
            it.returnType
                .isSubtypeOf(
                    ReferenceByName::class.createType(
                        arguments = listOf(
                            KTypeProjection(variance = KVariance.OUT, type = PossiblyNamed::class.createType())
                        ),
                        nullable = true
                    )
                )
        }.map { it as ReferenceByNameProperty }
}
