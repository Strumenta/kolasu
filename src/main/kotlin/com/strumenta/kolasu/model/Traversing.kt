package com.strumenta.kolasu.model

import java.util.*

/**
 * Some Kotlinization of Deques used as a stack.
 */
typealias Stack<T> = Deque<T>

fun <T> mutableStackOf(): Stack<T> = ArrayDeque()

fun <T> Stack<T>.pushAll(elements: Collection<T>) {
    elements.reversed().forEach(this::push)
}

fun <T> mutableStackOf(vararg elements: T): Stack<T> {
    val stack = mutableStackOf<T>()
    stack.pushAll(elements.asList())
    return stack
}

/**
 * @return walks the whole AST starting from this node, depth-first.
 */
fun Node.walk(): Sequence<Node> {
    val stack: Stack<Node> = mutableStackOf(this)
    return generateSequence {
        if (stack.peek() == null) {
            null
        } else {
            val next: Node = stack.pop()
            stack.pushAll(next.children)
            next
        }
    }
}

/**
 * Performs a post-order (or leaves-first) node traversal starting with a given node.
 */
fun Node.walkLeavesFirst(): Sequence<Node> {
    val nodesStack: Stack<List<Node>> = mutableStackOf()
    val cursorStack: Stack<Int> = ArrayDeque()
    var done = false

    fun nextFromLevel(): Node {
        val nodes: List<Node> = nodesStack.peek()
        val cursor = cursorStack.pop()
        cursorStack.push(cursor + 1)
        return nodes[cursor]
    }

    fun fillStackToLeaf(node: Node) {
        var currentNode: Node = node
        while (true) {
            val childNodes: List<Node> = currentNode.children
            if (childNodes.isEmpty()) {
                break
            }
            nodesStack.push(childNodes)
            cursorStack.push(0)
            currentNode = childNodes[0]
        }
    }
    fillStackToLeaf(this)
    return generateSequence {
        if (done) {
            null
        } else {
            val nodes: List<Node> = nodesStack.peek()
            val cursor = cursorStack.peek()
            val levelHasNext = cursor < nodes.size
            if (levelHasNext) {
                val node: Node = nodes[cursor]
                fillStackToLeaf(node)
                nextFromLevel()
            } else {
                nodesStack.pop()
                cursorStack.pop()
                val hasNext = !nodesStack.isEmpty()
                if (hasNext) {
                    nextFromLevel()
                } else {
                    done = true
                    this
                }
            }
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
 * @return all direct children of this node.
 */
fun Node.walkChildren(): Sequence<Node> {
    return sequence {
        nodeProperties.forEach { property ->
            when (val value = property.get(this@walkChildren)) {
                is Node -> yield(value as Node)
                is Collection<*> -> value.forEach { if (it is Node) yield(it as Node) }
            }
        }
    }
}

/**
 * @param walker a function that generates a sequence of nodes. By default this is the depth-first "walk" method. For post-order traversal, take "walkLeavesFirst"
 * @return walks the whole AST starting from the childnodes of this node.
 */
fun Node.walkDescendants(walker: (Node) -> Sequence<Node> = Node::walk): Sequence<Node> {
    return walker.invoke(this).filter { node -> node != this }
}
