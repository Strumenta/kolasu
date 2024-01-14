package com.strumenta.kolasu.model

import java.io.Serializable

interface Destination

data class CompositeDestination(
    val elements: List<Destination>,
) : Destination,
    Serializable

data class TextFileDestination(
    val range: Range?,
) : Destination,
    Serializable

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
