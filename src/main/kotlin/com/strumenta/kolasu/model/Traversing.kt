package com.strumenta.kolasu.model

import java.util.*
import kotlin.reflect.KFunction1

/**
 * @return walks the whole AST starting from this node, depth-first.
 */
fun Node.walk(): Sequence<Node> {
    val stack: Deque<Node> = ArrayDeque()
    stack.push(this)
    return generateSequence {
        if (stack.peek() == null) {
            null
        } else {
            val next: Node = stack.pop()
            val children: List<Node> = next.children
            children.reversed().forEach { child -> stack.push(child) }
            next
        }
    }
}

/**
 * @return the sequence of nodes from this.parent all the way up to the root node.
 * For this to work, assignParents() must have been called.
 */
fun Node.walkAncestors(): Sequence<Node> {
    var currentNode: Node? = this
    return generateSequence {
        currentNode = currentNode!!.parent
        currentNode
    }
}

/**
 * @param walker a function that generates a sequence of nodes. By default this is the depth-first "walk" method.
 * @return walks the whole AST starting from the childnodes of this node.
 */
fun Node.walkDescendants(walker: KFunction1<Node, Sequence<Node>> = Node::walk): Sequence<Node> {
    return walker.invoke(this).filter { node -> node != this }
}
