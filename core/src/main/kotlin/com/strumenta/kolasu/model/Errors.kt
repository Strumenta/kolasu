package com.strumenta.kolasu.model

import com.strumenta.kolasu.language.BaseStarLasuLanguage
import com.strumenta.kolasu.language.Concept
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
            ?: error?.message() ?: "Unspecified error node"

    private fun Throwable.message(): String {
        val cause = this.cause?.message()?.let { " -> $it" } ?: ""
        val explanation = if (this.message != null) ": " + this.message else ""
        return "${this.javaClass.simpleName}$explanation$cause"
    }

    override val concept: Concept
        get() = BaseStarLasuLanguage.astNode
}

fun NodeLike.errors(): Sequence<ErrorNode> = this.walkDescendants(ErrorNode::class)

fun NodeLike.findError(): ErrorNode? = this.errors().firstOrNull()
