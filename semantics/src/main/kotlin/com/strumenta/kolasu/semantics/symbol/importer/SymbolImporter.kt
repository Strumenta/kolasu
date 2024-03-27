package com.strumenta.kolasu.semantics.symbol.importer

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.semantics.symbol.provider.SymbolProvider
import com.strumenta.kolasu.semantics.symbol.repository.SymbolRepository
import com.strumenta.kolasu.traversing.walkDescendants

/**
 * Component supporting the import process of symbol description
 * from nodes into a [symbolRepository] using a [symbolProvider].
 *
 * @property symbolProvider the symbol provider to use during the import process
 * @property symbolRepository the repository where to store the obtained symbol descriptions
 **/
class SymbolImporter(
    private val symbolProvider: SymbolProvider,
    private val symbolRepository: SymbolRepository
) {

    /**
     * Imports symbols for all the nodes contained
     * in a given tree in the symbol repository starting from its [root].
     * @param root the root node of the tree
     **/
    fun importTree(root: Node) {
        this.importNode(root)
        root.walkDescendants().forEach(this::importTree)
    }

    /**
     * Imports the symbol for the given [node] in the symbol repository.
     * @param node the node from which to compute the symbol description to import
     **/
    fun importNode(node: Node) {
        this.symbolProvider.from(node)?.let(this.symbolRepository::store)
    }
}
