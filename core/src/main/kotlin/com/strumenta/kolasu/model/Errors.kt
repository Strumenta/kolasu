package com.strumenta.kolasu.model

import com.strumenta.kolasu.traversing.walkDescendants

/**
 * Generic implementation of [ErrorNode].
 */
class GenericErrorNode(
    error: Exception? = null,
    message: String? = null,
) : Node(),
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

fun NodeLike.errors(): Sequence<ErrorNode> = this.walkDescendants(ErrorNode::class)

fun NodeLike.findError(): ErrorNode? = this.errors().firstOrNull()
