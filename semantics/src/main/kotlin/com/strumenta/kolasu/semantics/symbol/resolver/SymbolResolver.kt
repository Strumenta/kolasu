package com.strumenta.kolasu.semantics.symbol.resolver

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.children
import com.strumenta.kolasu.model.getReferredType
import com.strumenta.kolasu.model.kReferenceByNameType
import com.strumenta.kolasu.model.nodeProperties
import com.strumenta.kolasu.semantics.scope.description.ScopeDescription
import com.strumenta.kolasu.semantics.scope.provider.ScopeProvider
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubtypeOf

/**
 * Component implementing the resolution of references to their corresponding target
 * according to the scoping rules defined by the [scopeProvider]. Local references
 * will be resolved to the corresponding node, while external references will be resolved
 * to the identifier of the corresponding node.
 *
 * @property scopeProvider the scope provider defining the scoping rules to apply
 **/
data class SymbolResolver(private val scopeProvider: ScopeProvider) {

    /**
     * Resolves all reference by names properties
     * for all nodes contained in a tree starting from the [root].
     * @param root the root of the tree for which to resolve all reference by names
     **/
    fun resolveTree(root: Node) {
        this.resolveNode(root)
        root.children.forEach(this::resolveTree)
    }

    /**
     * Resolves all reference by name properties of the given [node].
     * @param node the node for which to resolve all reference by names
     **/
    fun resolveNode(node: Node) {
        findScopeFrom(node).firstOrNull()?.let { scope ->
            node.referenceByNameProperties().forEach { scope.resolve(it.get(node), it.getReferredType()) }
        }
    }

    /**
     * Resolves the given [reference] for the given [node] using the [scopeProvider].
     * @param reference the reference property to resolve
     * @param node the node containing the reference to resolve
     **/
    fun <NodeTy : Node> resolveReference(node: NodeTy, reference: KProperty1<in NodeTy, ReferenceByName<*>?>) {
        this.findScopeFrom(node).firstOrNull()?.resolve(reference.get(node), reference.getReferredType())
    }

    private fun findScopeFrom(node: Node): Sequence<ScopeDescription> = sequence {
        scopeProvider.from(node)?.let { yield(it) }
        node.parent?.let { yieldAll(findScopeFrom(it)) }
    }

    /**
     * Retrieves all reference by name properties for [this] node.
     * @return list of all the reference by name properties of [this] ]node
     **/
    private fun Node.referenceByNameProperties(): List<KProperty1<Node, ReferenceByName<*>?>> {
        return this.nodeProperties
            .filter { it.returnType.isSubtypeOf(kReferenceByNameType()) }
            .mapNotNull { it as? KProperty1<Node, ReferenceByName<*>?> }
    }
}
