package com.strumenta.kolasu.model

import java.util.*

/**
 * The children, the children of those children, etc. The order is depth-first.
 */
fun Node.descendants(): Sequence<Node> {
    val stack: Deque<Node> = ArrayDeque()
    children.reversed().forEach { stack.push(it) }
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
