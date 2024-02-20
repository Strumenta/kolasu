package com.strumenta.kolasu.semantics.scope.provider.declarative

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.semantics.scope.description.ScopeDescription
import com.strumenta.kolasu.semantics.scope.provider.ScopeProvider
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSuperclassOf

inline fun <reified NodeTy : Node, PropertyType : KProperty1<in NodeTy, ReferenceByName<out PossiblyNamed>>> scopeFor(
    property: PropertyType,
    noinline specification: ScopeDescription.(DeclarativeScopeProviderRuleArguments<NodeTy>) -> Unit
): DeclarativeScopeProviderRule<NodeTy> {
    return DeclarativeScopeProviderRule(NodeTy::class, property.name, specification)
}

class DeclarativeScopeProvider(
    private val ignoreCase: Boolean = false,
    vararg rules: DeclarativeScopeProviderRule<out Node>
) : ScopeProvider {
    private val rules: List<DeclarativeScopeProviderRule<out Node>> = rules.sorted()

    override fun <NodeType : Node> scopeFor(
        node: NodeType,
        property: KProperty1<in NodeType, ReferenceByName<out PossiblyNamed>>
    ): ScopeDescription? {
        return this.rules
            .firstOrNull { it.canBeInvokedWith(node::class, property) }
            ?.invoke(this, node, this.ignoreCase)
    }
}

class DeclarativeScopeProviderRule<NodeTy : Node>(
    private val nodeType: KClass<NodeTy>,
    private val propertyName: String,
    private val specification: ScopeDescription.(DeclarativeScopeProviderRuleArguments<NodeTy>) -> Unit
) : (ScopeProvider, Node, Boolean) -> ScopeDescription, Comparable<DeclarativeScopeProviderRule<out Node>> {
    override fun invoke(
        scopeProvider: ScopeProvider,
        node: Node,
        ignoreCase: Boolean
    ): ScopeDescription {
        return ScopeDescription(ignoreCase).apply {
            @Suppress("UNCHECKED_CAST")
            (node as? NodeTy)?.let { this.specification(DeclarativeScopeProviderRuleArguments(it, scopeProvider)) }
        }
    }

    fun canBeInvokedWith(
        nodeType: KClass<*>,
        property: KProperty1<*, ReferenceByName<out PossiblyNamed>>
    ): Boolean {
        return this.nodeType.isSuperclassOf(nodeType) && this.propertyName == property.name
    }

    override fun compareTo(other: DeclarativeScopeProviderRule<out Node>): Int {
        return when {
            this.nodeType.isSuperclassOf(other.nodeType) -> 1
            other.nodeType.isSuperclassOf(this.nodeType) -> -1
            else -> this.describeForComparison() compareTo other.describeForComparison()
        }
    }

    private fun describeForComparison(): String {
        return "${this.nodeType.qualifiedName ?: ""}::${this.propertyName}"
    }
}

data class DeclarativeScopeProviderRuleArguments<NodeTy : Node>(
    val node: NodeTy,
    val scopeProvider: ScopeProvider
)
