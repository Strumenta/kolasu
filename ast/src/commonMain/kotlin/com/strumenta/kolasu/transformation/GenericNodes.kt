package com.strumenta.kolasu.transformation

import com.strumenta.kolasu.model.MPNode
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.Range
import com.strumenta.kolasu.traversing.children

/**
 * A generic AST node. We use it to represent parts of a source tree that we don't know how to translate yet.
 */
class GenericMPNode(
    parent: NodeLike? = null,
) : MPNode() {
    init {
        this.parent = parent
    }
}

fun NodeLike.findGenericNode(): GenericMPNode? =
    if (this is GenericMPNode) {
        this
    } else {
        this.children.firstNotNullOfOrNull {
            it.findGenericNode()
        }
    }

/**
 * Generic implementation of [ErrorNode].
 */
class GenericMPErrorNode(
    error: Exception? = null,
    message: String? = null,
) : MPNode(),
    ErrorNode {
    override val message: String =
        message
            ?: if (error != null) {
                val msg = if (error.message != null) ": " + error.message else ""
                "Exception ${error::class.qualifiedName}$msg"
            } else {
                "Unspecified error node"
            }
}

/**
 * An AST node that marks the presence of an error, for example a syntactic or semantic error in the original tree.
 */
interface ErrorNode {
    val message: String
    val range: Range?
}