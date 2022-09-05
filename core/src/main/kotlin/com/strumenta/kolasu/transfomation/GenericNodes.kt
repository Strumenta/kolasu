package com.strumenta.kolasu.transfomation

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.children

/**
 * A generic AST node. We use it to represent parts of a source tree that we don't know how to translate yet.
 */
class GenericNode(parent: Node? = null) : Node() {
    init {
        this.parent = parent
    }
}

fun Node.findGenericNode(): GenericNode? {
    return if (this is GenericNode) this else this.children.firstNotNullOfOrNull { it.findGenericNode() }
}
