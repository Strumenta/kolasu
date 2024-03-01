package com.strumenta.kolasu.semantics.symbol.resolver

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.children
import com.strumenta.kolasu.model.kReferenceByNameType
import com.strumenta.kolasu.model.nodeProperties
import com.strumenta.kolasu.semantics.scope.provider.ScopeProvider
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubtypeOf

/**
 * Symbol resolver instances can be used to resolve references within AST nodes
 * (i.e. `ReferenceByName` instances). Internally, the symbol resolver uses a scope provider
 * defining the language-specific scoping rules. The resolution process can be invoked for
 * specific node properties or for all properties of a given node (possibly its children as well).
 *
 * Symbol resolution can be executed invoking one of the following methods:
 * - `resolve(node, reference)`: to resolve a specific reference of the given node;
 * - `resolve(node, withChildren)`: to resolve all references of the given node
 * and its children if `withChildren` is set to `true`;
 *
 * In both cases, the symbol resolver will resolve references by performing an in-place update.
 * For each reference, the `referred` or `identifier` properties will be updated
 * if an entry is found in the corresponding scope.
 **/
open class SymbolResolver(
    private val scopeProvider: ScopeProvider,
) {
    /**
     * Attempts to resolve the given reference property of the given node.
     **/
    fun resolve(
        node: Node,
        reference: KProperty1<Node, ReferenceByName<PossiblyNamed>?>,
    ) {
        node.properties
            .find { it.name == reference.name }
            ?.let {
                @Suppress("UNCHECKED_CAST")
                it.value as ReferenceByName<PossiblyNamed>
            }
            ?.let { this.scopeProvider.scopeFor(node, reference).resolve(it) }
    }

    /**
     * Attempts to resolve all reference properties of the
     * given node and its children (if `withChildren` is `true`).
     **/
    fun resolve(
        node: Node,
        entireTree: Boolean = false,
    ) {
        node.references().forEach { reference -> this.resolve(node, reference) }
        if (entireTree) node.children.forEach { this.resolve(it, entireTree) }
    }

    /**
     * Retrieve all reference properties of a given node.
     **/
    private fun Node.references(): List<KProperty1<Node, ReferenceByName<PossiblyNamed>?>> {
        return this.nodeProperties
            .filter { it.returnType.isSubtypeOf(kReferenceByNameType()) }
            .mapNotNull {
                @Suppress("UNCHECKED_CAST")
                it as? KProperty1<Node, ReferenceByName<PossiblyNamed>?>
            }
    }
}
