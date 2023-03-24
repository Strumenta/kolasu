package com.strumenta.kolasu.model

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createType
import kotlin.reflect.full.functions
import kotlin.reflect.full.isSupertypeOf

interface ScopeProvider {

    fun <N : Node, C : N> scopeFor(context: C, reference: KProperty1<N, ReferenceByName<*>>): Scope {
        return this.tryPropertyScopeFor(context, reference)
            ?: this.tryTypeScopeFor(context, reference)
            ?: Scope() // delegate
    }

    private fun <N : Node, C : Node> tryPropertyScopeFor(
        context: C,
        reference: KProperty1<N, ReferenceByName<*>>,
    ): Scope? {
        return this.tryScopeFor(context, reference, this::propertyScopeFunctionName)
    }

    private fun <N : Node, C : Node> tryTypeScopeFor(context: C, reference: KProperty1<N, ReferenceByName<*>>): Scope? {
        return this.tryScopeFor(context, reference, this::typeScopeFunctionName)
    }

    private fun <N : Node, C : Node> tryScopeFor(
        context: C,
        reference: KProperty1<N, ReferenceByName<*>>,
        scopeFunctionName: (KProperty1<N, ReferenceByName<*>>) -> String,
    ): Scope? {
        return this.tryScopeFor(context, scopeFunctionName(reference))?.call(this, context)
            ?: context.parent?.let { this.tryScopeFor(it, scopeFunctionName(reference))?.call(this, it) }
    }

    @Suppress("unchecked_cast")
    private fun <C : Node> tryScopeFor(context: C, name: String): KFunction<Scope>? {
        return this::class.functions.asSequence()
            .filter { function -> function.name == name }
            .filter { function -> function.returnType.classifier == Scope::class }
            .filter { function -> function.parameters.size > 1 }
            .filter { function -> function.parameters[1].type.isSupertypeOf(context::class.createType()) }
            .sortedWith { left, right ->
                when {
                    left.parameters[1].type.isSupertypeOf(right.parameters[1].type) -> 1
                    right.parameters[1].type.isSupertypeOf(left.parameters[1].type) -> -1
                    else -> 0
                }
            }.firstOrNull() as KFunction<Scope>?
    }

    private fun <N : Node> propertyScopeFunctionName(property: KProperty1<N, ReferenceByName<*>>): String {
        val kClass: KClass<*> = property.parameters.first().type.classifier as KClass<*>
        return "scopeFor_${kClass.simpleName}_${property.name}"
    }

    private fun <N : Node> typeScopeFunctionName(property: KProperty1<N, ReferenceByName<*>>): String {
        val kClass: KClass<*> = property.returnType.classifier as KClass<*>
        return "scopeFor_${kClass.simpleName}"
    }
}

@Suppress("unused")
open class DefaultScopeProvider : ScopeProvider {
    fun scopeFor_Node(context: Node): Scope {
        println("scopeFor_Node")
        return Scope()
    }
}
