package com.strumenta.kolasu.model

/**
 * An AST node that marks the presence of an error, for example a syntactic or semantic error in the original tree.
 */
interface ErrorNode {
    val message: String
    val position: Position?
}

/**
 * Generic implementation of [ErrorNode].
 */
class GenericErrorNode(error: Exception? = null, message: String? = null) : Node(), ErrorNode {
    override val message: String = message ?: error?.message ?: "Unspecified error node"
}

fun Node.errors(): Sequence<ErrorNode> = this.walkDescendants(ErrorNode::class)

fun Node.findError(): ErrorNode? = this.errors().firstOrNull()
