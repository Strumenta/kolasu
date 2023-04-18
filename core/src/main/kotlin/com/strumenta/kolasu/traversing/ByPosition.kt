@file:JvmName("ProcessingByPosition")
package com.strumenta.kolasu.traversing

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Range

/**
 * @param range the position where to search for nodes
 * @param selfContained whether the starting node position contains the positions of all its children.
 * If **true** no further search will be performed in subtrees where the root node falls outside the given position.
 * If **false (default)** the research will cover all nodes from the starting node to the leaves.
 * @return the node most closely containing the given [range]. Null if none is found.
 * @see searchByPosition
 */
@JvmOverloads
fun Node.findByPosition(range: Range, selfContained: Boolean = false): Node? {
    return this.searchByPosition(range, selfContained).lastOrNull()
}

/**
 * @param range the position where to search for nodes
 * @param selfContained whether the starting node position contains the positions of all its children.
 * If **true**: no further search will be performed in subtrees where the root node falls outside the given position.
 * If **false (default)**: the search will cover all nodes from the starting node to the leaves.
 * In any case, the search stops at the first subtree found to be containing the position.
 * @return all nodes containing the given [range] using depth-first search. Empty list if none are found.
 */
@JvmOverloads
fun Node.searchByPosition(range: Range, selfContained: Boolean = false): Sequence<Node> {
    val contains = this.contains(range)
    if (!selfContained || contains) {
        if (children.isEmpty()) {
            return if (contains) sequenceOf(this) else emptySequence()
        } else {
            for (c in children) {
                val seq = c.searchByPosition(range, selfContained).iterator()
                if (seq.hasNext()) {
                    return sequence {
                        yield(this@searchByPosition)
                        yieldAll(seq)
                    }
                }
            }
            if (contains) {
                return sequenceOf(this)
            }
        }
    }
    return emptySequence()
}

/**
 * @param range the position within which the walk should remain
 * @return walks the AST within the given [range] starting from this node, depth-first.
 */
fun Node.walkWithin(range: Range): Sequence<Node> {
    return if (range.contains(this)) {
        sequenceOf(this) + this.children.walkWithin(range)
    } else if (this.overlaps(range)) {
        this.children.walkWithin(range)
    } else emptySequence()
}

/**
 * @param range the position within which the walk should remain
 * @return walks the AST within the given [range] starting from each node
 * and concatenates all results in a single sequence
 */
fun List<Node>.walkWithin(range: Range): Sequence<Node> {
    return this
        .map { it.walkWithin(range) }
        .reduceOrNull { previous, current -> previous + current } ?: emptySequence()
}
