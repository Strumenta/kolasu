package com.strumenta.kolasu.symbolresolution

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.traversing.walkChildren
import com.strumenta.kolasu.validation.Issue
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

data class DeclarativeSymbolResolver(val issues: MutableList<Issue> = mutableListOf()) : LocalSymbolResolver() {

    val classScopeDefinitions: ClassScopeDefinitions = mutableMapOf()
    val propertyScopeDefinitions: PropertyScopeDefinitions = mutableMapOf()

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
        return tryGetScope(classScopeDefinitions[referenceType], context)
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
        this.classScopeDefinitions.computeIfAbsent(nodeType) { mutableListOf() }
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
    issues: MutableList<Issue> = mutableListOf(),
    init: DeclarativeSymbolResolver.() -> Unit,
) = DeclarativeSymbolResolver(issues).apply(init)
