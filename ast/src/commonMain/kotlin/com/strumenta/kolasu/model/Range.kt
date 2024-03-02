package com.strumenta.kolasu.model

/**
 * Tests whether the given node is contained in the interval represented by this object.
 * @param node the node
 */
fun Range.contains(node: NodeLike): Boolean = this.contains(node.range)

/**
 * Utility function to create a Range
 */
fun range(
    startLine: Int,
    startCol: Int,
    endLine: Int,
    endCol: Int,
) = Range(
    Point(startLine, startCol),
    Point(endLine, endCol),
)

fun NodeLike.isBefore(other: NodeLike): Boolean = range!!.start.isBefore(other.range!!.start)

val NodeLike.startLine: Int?
    get() = this.range?.start?.line

val NodeLike.endLine: Int?
    get() = this.range?.end?.line
