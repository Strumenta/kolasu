package com.strumenta.kolasu.transformation

import com.strumenta.kolasu.model.ASTNode
import com.strumenta.kolasu.traversing.children

/**
 * A generic AST node. We use it to represent parts of a source tree that we don't know how to translate yet.
 */
class GenericNode(parent: ASTNode? = null) : ASTNode() {
    init {
        this.parent = parent
    }
}

fun ASTNode.findGenericNode(): GenericNode? {
    return if (this is GenericNode) this else this.children.firstNotNullOfOrNull { it.findGenericNode() }
}
