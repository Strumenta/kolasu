package com.strumenta.kolasu.semantics.symbol.resolver

import com.strumenta.kolasu.model.*
import com.strumenta.kolasu.semantics.scope.provider.ReferenceNode
import com.strumenta.kolasu.semantics.scope.provider.ScopeProvider

/**
 * Symbol resolver instances can be used to resolve references within AST nodes
 * (i.e. `ReferenceByName` instances). Internally, the symbol resolver uses a scope provider
 * defining the language-specific scoping rules. The resolution process can be invoked for
 * specific node properties or for all properties of a given node (possibly its children as well).
 *
 * Symbol resolution can be executed invoking one of the following methods:
 * - `resolve(node, reference)`: to resolve a specific reference of the given node;
 * - `resolve(node, entireTree)`: to resolve all references of the given node
 * and its children if `entireTree` is set to `true`;
 *
 * In both cases, the symbol resolver will resolve references by performing an in-place update.
 * For each reference, the `referred` or `identifier` properties will be updated
 * if an entry is found in the corresponding scope.
 **/
data class SymbolResolver(private val scopeProvider: ScopeProvider) {

    fun resolveTree(tree: Node) {
        this.resolveNode(tree)
        tree.children.forEach(this::resolveTree)
    }

    fun resolveNode(node: Node) {
        node.children.filterIsInstance<ReferenceNode<*>>().forEach(this::resolveReference)
    }

    fun resolveReference(reference: ReferenceNode<*>) {
        this.scopeProvider.scopeFor(reference).resolve(reference)
    }
}
