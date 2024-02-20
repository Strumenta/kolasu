package com.strumenta.kolasu.semantics.symbol.resolver

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.children
import com.strumenta.kolasu.model.kReferenceByNameType
import com.strumenta.kolasu.model.nodeProperties
import com.strumenta.kolasu.semantics.node.repository.NodeRepository
import com.strumenta.kolasu.semantics.scope.provider.ScopeProvider
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubtypeOf

open class SymbolResolver(
    private val scopeProvider: ScopeProvider,
    private val nodeRepository: NodeRepository
) {
    fun resolve(
        node: Node,
        property: KProperty1<Node, ReferenceByName<PossiblyNamed>>
    ) {
        @Suppress("UNCHECKED_CAST")
        val propertyValue: ReferenceByName<PossiblyNamed>? =
            node.properties
                .find { it.name == property.name }
                ?.value as? ReferenceByName<PossiblyNamed>?
        propertyValue
            ?.let { this.scopeProvider.scopeFor(node, property)?.resolve(it.name) }
            ?.let { this.nodeRepository.load(it) }
            ?.let { propertyValue.referred = it as? PossiblyNamed }
    }

    fun resolve(
        node: Node,
        withChildren: Boolean = false
    ) {
        node.references().forEach { reference -> this.resolve(node, reference) }
        if (withChildren) node.children.forEach { this.resolve(it) }
    }

    private fun Node.references(): List<KProperty1<Node, ReferenceByName<PossiblyNamed>>> {
        @Suppress("UNCHECKED_CAST")
        return this.nodeProperties
            .filter { it.returnType.isSubtypeOf(kReferenceByNameType()) }
            .mapNotNull { it as? KProperty1<Node, ReferenceByName<PossiblyNamed>> }
    }
}
