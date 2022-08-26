package com.strumenta.kolasu.model

import java.util.IdentityHashMap
import java.util.UUID

/**
 * @param walker a function that generates a sequence of nodes. By default this is the depth-first "walk" method.
 * @param idProvider a function that takes a node and returns a String identifier representing it
 * For post-order traversal, take "walkLeavesFirst".
 * @return walks the whole AST starting from the given node and associates each visited node with a generated id
 **/
fun Node.computeIds(
    walker: (Node) -> Sequence<Node> = Node::walk,
    idProvider: (Node) -> String = { UUID.randomUUID().toString() }
): IdentityHashMap<Node, String> {
    val idsMap = IdentityHashMap<Node, String>()
    walker.invoke(this).forEach { idsMap[it] = idProvider(it) }
    return idsMap
}
