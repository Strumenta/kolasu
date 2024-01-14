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
    val node: INode,
) : Destination

operator fun MutableList<Destination>.plusAssign(node: INode) {
    this.add(NodeDestination(node))
}

operator fun MutableList<Destination>.minusAssign(node: INode) {
    this.remove(NodeDestination(node))
}

operator fun List<Destination>.contains(node: INode): Boolean = NodeDestination(node) in this
