package com.strumenta.kolasu.model

import com.strumenta.kolasu.utils.memoize
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSuperclassOf

class ScopeDefinition(val contextType: KClass<*>, scopeFunction: ScopeFunction<Node>) {
    val scopeFunction: ScopeFunction<Node> = scopeFunction.memoize()
}

typealias ScopeFunction<N> = (N) -> Scope?
typealias PropertyTypeScopeDefinitions = MutableMap<KClass<*>, MutableList<ScopeDefinition>>
typealias PropertyScopeDefinitions = MutableMap<ReferenceByNameProperty<*>, MutableList<ScopeDefinition>>
typealias ReferenceByNameProperty<ContainerType> = KProperty1<ContainerType, ReferenceByName<*>>

fun declarativeScopeProvider(init: DeclarativeScopeProvider.() -> Unit): DeclarativeScopeProvider {
    return DeclarativeScopeProvider().apply(init)
}

interface ScopeProvider {
    fun getScope(context: Node, reference: ReferenceByNameProperty<*>): Scope?
}

class DeclarativeScopeProvider : ScopeProvider {

    val propertyScopeDefinitions: PropertyScopeDefinitions = mutableMapOf()
    val propertyTypeScopeDefinitions: PropertyTypeScopeDefinitions = mutableMapOf()

    override fun getScope(context: Node, reference: ReferenceByNameProperty<*>): Scope? {
        return this.tryGetScopeForProperty(context, reference)
            ?: this.tryGetScopeForPropertyType(context, reference)
    }

    private tailrec fun tryGetScopeForProperty(context: Node, reference: ReferenceByNameProperty<*>): Scope? {
        return this.tryGetScope(this.propertyScopeDefinitions[reference], context)
            ?: if (context.parent == null) { null } else {
                return tryGetScopeForProperty(context.parent!!, reference)
            }
    }

    private tailrec fun tryGetScopeForPropertyType(context: Node, reference: ReferenceByNameProperty<*>): Scope? {
        return tryGetScope(propertyTypeScopeDefinitions[reference.returnType.classifier], context)
            ?: if (context.parent == null) { null } else {
                return tryGetScopeForPropertyType(context.parent!!, reference)
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
        crossinline scopeFunction: ScopeFunction<ContextType>,
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

    inline fun <ContainerType : Node, reified ContextType : Node> scopeFor(
        reference: ReferenceByNameProperty<ContainerType>,
        crossinline scopeDefinition: ScopeFunction<ContextType>,
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
