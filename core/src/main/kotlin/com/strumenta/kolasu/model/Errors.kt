package com.strumenta.kolasu.model

import com.strumenta.kolasu.traversing.walkDescendants

/**
 * An AST node that marks the presence of an error, for example a syntactic or semantic error in the original tree.
 */
interface ErrorNode {
    val message: String
    val range: Range?
}

/**
 * Generic implementation of [ErrorNode].
 */
class GenericErrorNode(error: Exception? = null, message: String? = null) : Node(), ErrorNode {
    override val message: String = message
        ?: if (error != null) {
            val msg = if (error.message != null) ": " + error.message else ""
            "Exception ${error::class.qualifiedName}$msg"
        } else {
            "Unspecified error node"
        }
}

fun INode.errors(): Sequence<ErrorNode> = this.walkDescendants(ErrorNode::class)

fun INode.findError(): ErrorNode? = this.errors().firstOrNull()
