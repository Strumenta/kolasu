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
    override val message: String
    init {
        if (message != null) {
            this.message = message
        } else if (error != null) {
            val msg = if (error.message != null) ": " + error.message else ""
            this.message = "Exception ${error::class.qualifiedName}$msg"
        } else {
            this.message = "Unspecified error node"
        }
    }
}

fun Node.errors(): Sequence<ErrorNode> = this.walkDescendants(ErrorNode::class)

fun Node.findError(): ErrorNode? = this.errors().firstOrNull()
