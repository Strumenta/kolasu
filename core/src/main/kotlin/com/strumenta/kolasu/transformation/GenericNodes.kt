package com.strumenta.kolasu.transformation

import com.strumenta.kolasu.model.INode
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.traversing.children

/**
 * A generic AST node. We use it to represent parts of a source tree that we don't know how to translate yet.
 */
class GenericNode(
    parent: INode? = null,
) : Node() {
    init {
        this.parent = parent
    }
}

fun INode.findGenericNode(): GenericNode? =
    if (this is GenericNode) {
        this
    } else {
        this.children.firstNotNullOfOrNull {
            it.findGenericNode()
        }
    }
