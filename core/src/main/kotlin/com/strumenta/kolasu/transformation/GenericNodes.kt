package com.strumenta.kolasu.transformation

import com.strumenta.kolasu.language.Concept
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.traversing.children

/**
 * A generic AST node. We use it to represent parts of a source tree that we don't know how to translate yet.
 */
@Deprecated("To be removed in Kolasu 1.6")
class GenericNode(
    parent: NodeLike? = null,
    val specifiedConcept: Concept? = null,
) : Node() {
    init {
        this.parent = parent
    }

    override val concept: Concept
        get() = specifiedConcept ?: throw IllegalStateException("No specified concept for this GenericNode")
}

@Deprecated("To be removed in Kolasu 1.6")
fun NodeLike.findGenericNode(): GenericNode? =
    if (this is GenericNode) {
        this
    } else {
        this.children.firstNotNullOfOrNull {
            it.findGenericNode()
        }
    }
