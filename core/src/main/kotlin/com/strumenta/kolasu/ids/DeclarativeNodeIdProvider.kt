package com.strumenta.kolasu.ids

import com.strumenta.kolasu.model.NodeLike
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf

/**
 * Declarative node id provider instances allow to define
 * language-specific or custom identifier construction policies.
 *
 * ```kotlin
 * object MyDeclarativeNodeIdProvider: DeclarativeNodeIdProvider(
 *     idFor(PackageDeclaration::class) { it.node.name }
 *     idFor(ClassDeclaration::class) { (node: ClassDeclaration, identifiers: NodeIdProvider) ->
 *         val packageIdentifier = node.findAncestorOfType(PackageDeclaration::class)?.let { identifiers.idFor(it) } ?: "__none__"
 *         "${packageIdentifier}::${node.name}"
 *     }
 * )
 * ```
 **/
open class DeclarativeNodeIdProvider(
    vararg rules: DeclarativeNodeIdProviderRule<out NodeLike>,
) : SemanticNodeIDProvider {
    private val rules: List<DeclarativeNodeIdProviderRule<out NodeLike>> = rules.sorted()

    override fun hasSemanticIdentity(kNode: NodeLike): Boolean {
        return tryToGetSemanticID(kNode) != null
    }

    override fun semanticID(kNode: NodeLike): String {
        return tryToGetSemanticID(kNode)
            ?: throw RuntimeException("Cannot find rule for node type: ${kNode::class.qualifiedName}")
    }

    private fun tryToGetSemanticID(kNode: NodeLike): String? {
        return this.rules.firstOrNull { it.canBeInvokedWith(kNode::class) }?.invoke(this, kNode)
    }
}

/**
 * Utility function to define declarative node id provider rules.
 * Can be used whenever listing the rules in the DeclarativeNodeIdProvider constructor.
 **/
inline fun <reified NodeTy : NodeLike> idFor(
    noinline specification: SemanticNodeIDProvider.(NodeTy) -> String,
): DeclarativeNodeIdProviderRule<NodeTy> = DeclarativeNodeIdProviderRule(NodeTy::class, specification)

/**
 * Class representing a single rule of a DeclarativeNodeIdProvider.
 **/
class DeclarativeNodeIdProviderRule<NodeTy : NodeLike>(
    private val nodeType: KClass<NodeTy>,
    private val specification: SemanticNodeIDProvider.(NodeTy) -> String,
) : Comparable<DeclarativeNodeIdProviderRule<*>>,
    (SemanticNodeIDProvider, NodeLike) -> String {
    override fun invoke(
        nodeIdProvider: SemanticNodeIDProvider,
        node: NodeLike,
    ): String {
        @Suppress("UNCHECKED_CAST")
        return nodeIdProvider.specification(node as NodeTy)
    }

    fun canBeInvokedWith(type: KClass<*>): Boolean = this.nodeType.isSuperclassOf(type)

    override fun compareTo(other: DeclarativeNodeIdProviderRule<*>): Int =
        when {
            this.nodeType.isSuperclassOf(other.nodeType) -> 1
            this.nodeType.isSubclassOf(other.nodeType) -> -1
            else -> (this.nodeType.qualifiedName ?: "") compareTo (other.nodeType.qualifiedName ?: "")
        }
}
