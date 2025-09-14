package com.strumenta.kolasu.transformation

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.traversing.children

/**
 * A generic AST node. We use it to represent parts of a source tree that we don't know how to translate yet.
 */
@Deprecated("To be removed in Kolasu 2.0")
class GenericNode(
    parent: Node? = null,
) : Node() {
    init {
        this.parent = parent
    }
}

@Deprecated("To be removed in Kolasu 2.0")
fun Node.findGenericNode(): GenericNode? =
    if (this is GenericNode) {
        this
    } else {
        this.children.firstNotNullOfOrNull {
            it.findGenericNode()
        }
    }
