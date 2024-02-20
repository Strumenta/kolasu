package com.strumenta.kolasu.semantics.symbol.importer

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.children
import com.strumenta.kolasu.semantics.symbol.provider.SymbolProvider
import com.strumenta.kolasu.semantics.symbol.repository.SymbolRepository

open class SymbolImporter(
    private val symbolProvider: SymbolProvider,
    private val symbolRepository: SymbolRepository
) {
    fun import(
        node: Node,
        withChildren: Boolean = false
    ) {
        this.symbolProvider.symbolFor(node)
            ?.let { this.symbolRepository.store(it) }
        node.children.forEach { this.import(it, withChildren) }
    }
}
