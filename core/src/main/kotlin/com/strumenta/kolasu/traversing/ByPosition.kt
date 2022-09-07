package com.strumenta.kolasu.traversing

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Position

/**
 * @param position the position where to search for nodes
 * @param selfContained whether the starting node position contains the positions of all its children.
 * If **true** no further search will be performed in subtrees where the root node falls outside the given position.
 * If **false (default)** the research will cover all nodes from the starting node to the leaves.
 * @return the nearest node to the given [position]. Null if none is found.
 * @see searchByPosition
 */
@JvmOverloads
fun Node.findByPosition(position: Position, selfContained: Boolean = false): Node? {
    return this.searchByPosition(position, selfContained).lastOrNull()
}

/**
 * @param position the position where to search for nodes
 * @param selfContained whether the starting node position contains the positions of all its children.
 * If **true**: no further search will be performed in subtrees where the root node falls outside the given position.
 * If **false (default)**: the research will cover all nodes from the starting node to the leaves.
 * @return all nodes contained within the given [position] using depth-first search. Empty list if none are found.
 */
@JvmOverloads
fun Node.searchByPosition(position: Position, selfContained: Boolean = false): Sequence<Node> {
    return if (selfContained) {
        this.walkWithin(position)
    } else {
        this.walk().filter { position.contains(it) }
    }
}

/**
 * @param position the position within which the walk should remain
 * @return walks the AST within the given [position] starting from this node, depth-first.
 */
fun Node.walkWithin(position: Position): Sequence<Node> {
    return if (position.contains(this)) {
        sequenceOf(this) + this.children.walkWithin(position)
    } else if (this.contains(position)) {
        this.children.walkWithin(position)
    } else emptySequence<Node>()
}

/**
 * @param position the position within which the walk should remain
 * @return walks the AST within the given [position] starting from each node
 * and concatenates all results in a single sequence
 */
fun List<Node>.walkWithin(position: Position): Sequence<Node> {
    return this
        .map { it.walkWithin(position) }
        .reduceOrNull { previous, current -> previous + current } ?: emptySequence()
}
