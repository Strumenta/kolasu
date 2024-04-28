@file:JvmName("ProcessingStructurally")

package com.strumenta.kolasu.traversing

import com.strumenta.kolasu.model.NodeLike
import kotlin.jvm.JvmName

/**
 * @return the sequence of nodes from this.parent all the way up to the root node.
 * For this to work, assignParents() must have been called.
 */
fun NodeLike.walkAncestors(): Sequence<NodeLike> {
    var currentNode: NodeLike? = this
    return generateSequence {
        currentNode = currentNode!!.parent
        currentNode
    }
}

/**
 * @return all direct children of this node.
 */
fun NodeLike.walkChildren(includeDerived: Boolean = false): Sequence<NodeLike> {
    return sequence {
        (
            concept.allContainments.filter { includeDerived || !it.derived }
        ).forEach { containment ->
            when (val value = containment.value(this@walkChildren)) {
                is NodeLike -> yield(value)
                is Collection<*> -> value.forEach { if (it is NodeLike) yield(it) }
            }
        }
    }
}

typealias ASTWalker = (NodeLike) -> Sequence<NodeLike>

/**
 * @return all direct children of this node.
 */
val NodeLike.children: List<NodeLike>
    get() {
        return walkChildren().toList()
    }
