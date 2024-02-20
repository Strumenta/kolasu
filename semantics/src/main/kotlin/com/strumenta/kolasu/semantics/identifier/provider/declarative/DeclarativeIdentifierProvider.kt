package com.strumenta.kolasu.semantics.identifier.provider.declarative

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.semantics.identifier.provider.IdentifierProvider
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf

inline fun <reified NodeTy : Node> identifierFor(
    noinline specification: (DeclarativeIdentifierProviderRuleArguments<NodeTy>) -> String?
): DeclarativeIdentifierProviderRule<NodeTy> {
    return DeclarativeIdentifierProviderRule(NodeTy::class, specification)
}

open class DeclarativeIdentifierProvider(
    vararg rules: DeclarativeIdentifierProviderRule<out Node>
) : IdentifierProvider {
    private val rules: List<DeclarativeIdentifierProviderRule<out Node>> = rules.sorted()

    override fun <NodeTy : Node> getIdentifierFor(
        node: NodeTy,
        typedAs: KClass<in NodeTy>?
    ): String? {
        return this.rules
            .firstOrNull { it.canBeInvokedWith(typedAs ?: node::class) }
            ?.invoke(this, node)
    }
}

class DeclarativeIdentifierProviderRule<NodeTy : Node>(
    private val nodeType: KClass<NodeTy>,
    private val specification: (DeclarativeIdentifierProviderRuleArguments<NodeTy>) -> String?
) : Comparable<DeclarativeIdentifierProviderRule<*>>, (IdentifierProvider, Node) -> String? {
    override fun invoke(
        identifierProvider: IdentifierProvider,
        node: Node
    ): String? {
        @Suppress("UNCHECKED_CAST")
        return (node as? NodeTy)
            ?.let { DeclarativeIdentifierProviderRuleArguments(it, identifierProvider) }
            ?.let(specification)
    }

    fun canBeInvokedWith(withType: KClass<*>): Boolean {
        return this.nodeType.isSuperclassOf(withType)
    }

    override fun compareTo(other: DeclarativeIdentifierProviderRule<*>): Int {
        return when {
            this.nodeType.isSuperclassOf(other.nodeType) -> 1
            this.nodeType.isSubclassOf(other.nodeType) -> -1
            else -> (this.nodeType.qualifiedName ?: "") compareTo (other.nodeType.qualifiedName ?: "")
        }
    }
}

data class DeclarativeIdentifierProviderRuleArguments<NodeTy : Node>(
    val node: NodeTy,
    val identifierProvider: IdentifierProvider
)
