package com.strumenta.kolasu.ids

import com.strumenta.kolasu.model.Node
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
    vararg rules: DeclarativeNodeIdProviderRule<out Node>
) : NodeIdProvider {
    private val rules: List<DeclarativeNodeIdProviderRule<out Node>> = rules.sorted()
    override fun idUsingCoordinates(kNode: Node, coordinates: Coordinates): String {
        return this.rules.firstOrNull { it.canBeInvokedWith(kNode::class) }?.invoke(this, kNode)
            ?: throw RuntimeException("Cannot find rule for node type: ${kNode::class.qualifiedName}")
    }
}

/**
 * Utility function to define declarative node id provider rules.
 * Can be used whenever listing the rules in the DeclarativeNodeIdProvider constructor.
 **/
inline fun <reified NodeTy : Node> idFor(
    noinline specification: NodeIdProvider.(NodeTy) -> String
): DeclarativeNodeIdProviderRule<NodeTy> = DeclarativeNodeIdProviderRule(NodeTy::class, specification)

/**
 * Class representing a single rule of a DeclarativeNodeIdProvider.
 **/
class DeclarativeNodeIdProviderRule<NodeTy : Node>(
    private val nodeType: KClass<NodeTy>,
    private val specification: NodeIdProvider.(NodeTy) -> String
) : Comparable<DeclarativeNodeIdProviderRule<*>>, (NodeIdProvider, Node) -> String {
    override fun invoke(
        nodeIdProvider: NodeIdProvider,
        node: Node
    ): String {
        @Suppress("UNCHECKED_CAST")
        return nodeIdProvider.specification(node as NodeTy)
    }

    fun canBeInvokedWith(type: KClass<*>): Boolean {
        return this.nodeType.isSuperclassOf(type)
    }

    override fun compareTo(other: DeclarativeNodeIdProviderRule<*>): Int {
        return when {
            this.nodeType.isSuperclassOf(other.nodeType) -> 1
            this.nodeType.isSubclassOf(other.nodeType) -> -1
            else -> (this.nodeType.qualifiedName ?: "") compareTo (other.nodeType.qualifiedName ?: "")
        }
    }
}
