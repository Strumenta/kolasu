package com.strumenta.kolasu.semantics.services

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.kReferenceByNameProperties
import com.strumenta.kolasu.semantics.linking.ReferenceResolver
import com.strumenta.kolasu.traversing.walk
import kotlin.reflect.KProperty1

/**
 * Provides support for resolving AST references.
 * @property referenceResolver the component used to resolve references
 **/
class SymbolResolver(private val referenceResolver: ReferenceResolver) {

    /**
     * Resolve all references in the given [tree] using the [referenceResolver] rules.
     * @param tree the tree where to resolve all references
     **/
    fun resolveTree(tree: Node) {
        tree.walk().forEach(this::resolveNode)
    }

    /**
     * Resolves all references in the given [node] using the [referenceResolver] rules.
     * @param node the node where to resolve all references
     **/
    private fun <N : Node> resolveNode(node: N) {
        node.kReferenceByNameProperties()
            .map { it as KProperty1<N, ReferenceByName<PossiblyNamed>> }
            .forEach {
                this.referenceResolver.resolve(node, it)
                    ?.let { target -> it.get(node).referred = target as? PossiblyNamed }
            }
    }
}
