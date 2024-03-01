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
        entireTree: Boolean = false
    ) {
        this.symbolRepository.store(this.symbolProvider.symbolFor(node))
        node.children.forEach { this.import(it, entireTree) }
    }
}
