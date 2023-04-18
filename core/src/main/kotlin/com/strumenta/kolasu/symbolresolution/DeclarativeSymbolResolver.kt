package com.strumenta.kolasu.linking

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.nodeProperties
import com.strumenta.kolasu.traversing.walkChildren
import com.strumenta.kolasu.utils.memoize
import com.strumenta.kolasu.validation.Issue
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.isSuperclassOf

private typealias Issues = MutableList<Issue>

data class DeclarativeSymbolResolver(val issues: Issues = mutableListOf()) : LocalSymbolResolver() {

    val propertyScopeDefinitions: PropertyScopeDefinitions = mutableMapOf()
    val propertyTypeScopeDefinitions: PropertyTypeScopeDefinitions = mutableMapOf()

    override fun resolveSymbols(root: Node): List<Issue> {
        this.resolveNode(node = root, children = true)
        return this.issues
    }

    fun resolveNode(node: Node, children: Boolean = false) {
        node.referenceByNameProperties().forEach { resolveProperty(it, node) }
        if (children) { node.walkChildren().forEach { resolveNode(it, true) } }
    }

    @Suppress("unchecked_cast")
    fun resolveProperty(property: ReferenceByNameProperty, context: Node) {
        (context.properties.find { it.name == property.name }!!.value as ReferenceByName<Symbol>?)
            ?.apply { this.referred = getScope(property, context)?.resolve(this.name, property.getReferredType()) }
    }

    fun getScope(property: ReferenceByNameProperty, context: Node): Scope? =
        this.tryGetScopeForProperty(property, context) ?: this.tryGetScopeForPropertyType(property, context)

    private tailrec fun tryGetScopeForProperty(reference: ReferenceByNameProperty, context: Node): Scope? {
        return this.tryGetScope(this.propertyScopeDefinitions[reference], context)
            ?: if (context.parent == null) { null } else {
                return tryGetScopeForProperty(reference, context.parent!!)
            }
    }

    private tailrec fun tryGetScopeForPropertyType(reference: ReferenceByNameProperty, context: Node): Scope? {
        val referenceType = reference.returnType.arguments[0].type!!.classifier!!
        return tryGetScope(propertyTypeScopeDefinitions[referenceType], context)
            ?: if (context.parent == null) { null } else {
                return tryGetScopeForPropertyType(reference, context.parent!!)
            }
    }

    private fun tryGetScope(scopeDefinitions: List<ScopeDefinition>?, context: Node): Scope? {
        return scopeDefinitions
            ?.filter { scopeDefinition -> scopeDefinition.contextType.isSuperclassOf(context::class) }
            ?.sortedWith { left, right ->
                when {
                    left.contextType.isSuperclassOf(right.contextType) -> 1
                    right.contextType.isSuperclassOf(left.contextType) -> -1
                    else -> 0
                }
            }?.firstOrNull()?.scopeFunction?.invoke(context)
    }

    inline fun <reified ContextType : Node> scopeFor(
        nodeType: KClass<*>,
        crossinline scopeFunction: (ContextType) -> Scope?,
    ) {
        this.propertyTypeScopeDefinitions.computeIfAbsent(nodeType) { mutableListOf() }
            .add(
                ScopeDefinition(
                    contextType = ContextType::class,
                    scopeFunction = { context: Node ->
                        if (context is ContextType) scopeFunction(context) else null
                    },
                ),
            )
    }

    inline fun <reified ContextType : Node> scopeFor(
        reference: ReferenceByNameProperty,
        crossinline scopeDefinition: (ContextType) -> Scope?,
    ) {
        this.propertyScopeDefinitions.computeIfAbsent(reference) { mutableListOf() }
            .add(
                ScopeDefinition(
                    contextType = ContextType::class,
                    scopeFunction = { context: Node ->
                        if (context is ContextType) scopeDefinition(context) else null
                    },
                ),
            )
    }
}

fun declarativeSymbolResolver(
    issues: Issues = mutableListOf(),
    init: DeclarativeSymbolResolver.() -> Unit,
) = DeclarativeSymbolResolver(issues).apply(init)

@Suppress("unchecked_cast")
private fun Node.referenceByNameProperties(): Collection<ReferenceByNameProperty> {
    return this.nodeProperties
        .filter {
            it.returnType
                .isSubtypeOf(
                    ReferenceByName::class.createType(
                        arguments = listOf(
                            KTypeProjection(variance = KVariance.OUT, type = Symbol::class.createType()),
                        ),
                        nullable = true,
                    ),
                )
        }.map { it as ReferenceByNameProperty }
}

@Suppress("unchecked_cast")
private fun ReferenceByNameProperty.getReferredType(): KClass<out Symbol> {
    return this.returnType.arguments[0].type!!.classifier!! as KClass<out Symbol>
}

class ScopeDefinition(val contextType: KClass<out Node>, scopeFunction: (Node) -> Scope?) {
    val scopeFunction: (Node) -> Scope? = scopeFunction.memoize()
}

typealias PropertyTypeScopeDefinitions = MutableMap<KClass<*>, MutableList<ScopeDefinition>>
typealias PropertyScopeDefinitions = MutableMap<ReferenceByNameProperty, MutableList<ScopeDefinition>>

interface Symbol : PossiblyNamed

typealias Symbols = MutableMap<String, MutableList<Symbol>>

// TODO handle case-sensitivity : boolean
data class Scope(var parent: Scope? = null, val symbols: Symbols = mutableMapOf()) {
    fun define(symbol: Symbol) {
        val name: String = symbol.name ?: throw IllegalArgumentException("The given symbol must have a name")
        this.symbols.computeIfAbsent(name) { mutableListOf() }.add(symbol)
    }

    fun resolve(name: String, type: KClass<out Symbol> = Symbol::class): Symbol? {
        // retrieve multiple symbols -> error if more than one
        return this.symbols.getOrDefault(name, mutableListOf()).find { type.isInstance(it) }
            ?: this.parent?.resolve(name, type)
    }
}

typealias ReferenceByNameProperty = KProperty1<out Node, ReferenceByName<out Symbol>?>
