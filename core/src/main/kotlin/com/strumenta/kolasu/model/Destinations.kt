package com.strumenta.kolasu.model

data class CompositeDestination(
    val elements: List<Destination>,
) : Destination

data class TextFileDestination(
    val range: Range?,
) : Destination

data class NodeDestination(
    val node: NodeLike,
) : Destination

operator fun MutableList<Destination>.plusAssign(node: NodeLike) {
    this.add(NodeDestination(node))
}

operator fun MutableList<Destination>.minusAssign(node: NodeLike) {
    this.remove(NodeDestination(node))
}

operator fun List<Destination>.contains(node: NodeLike): Boolean = NodeDestination(node) in this
