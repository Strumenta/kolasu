package com.strumenta.kolasu.semantics.services

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.semantics.indexing.IndexWriter
import com.strumenta.kolasu.traversing.walk

/**
 * Provides support for exporting AST symbols into indices.
 * @property indexWriter the component used to write symbols in the index
 **/
class SymbolIndexer(private val indexWriter: IndexWriter = IndexWriter()) {
    /**
     * Index all symbols from the given [tree] using the [indexWriter] rules.
     * @param tree the tree from which to export symbols
     **/
    fun indexTree(tree: Node) {
        tree.walk().forEach(this.indexWriter::write)
    }
}
