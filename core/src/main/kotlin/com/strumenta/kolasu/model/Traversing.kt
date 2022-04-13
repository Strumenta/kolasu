package com.strumenta.kolasu.model

import java.util.*
import kotlin.reflect.KClass

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
        if (stack.isEmpty()) {
            null
        } else {
            val next: Node = stack.pop()
            stack.pushAll(next.children)
            next
        }
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
        this@walkChildren.properties.forEach { property ->
            when (val value = property.value) {
                is Node -> yield(value)
                is Collection<*> -> value.forEach { if (it is Node) yield(it) }
            }
        }
    }
}

/**
 * @param walker a function that generates a sequence of nodes. By default this is the depth-first "walk" method.
 * For post-order traversal, take "walkLeavesFirst"
 * @return walks the whole AST starting from the childnodes of this node.
 */
@JvmOverloads
fun Node.walkDescendants(walker: (Node) -> Sequence<Node> = Node::walk): Sequence<Node> {
    return walker.invoke(this).filter { node -> node != this }
}

@JvmOverloads
fun <N : Any> Node.walkDescendants(type: KClass<N>, walker: (Node) -> Sequence<Node> = Node::walk): Sequence<N> {
    return walkDescendants(walker).filterIsInstance(type.java)
}

/**
 * Note that type T is not strictly forced to be a Node. This is intended to support
 * interfaces like `Statement` or `Expression`. However, being an ancestor the returned
 * value is guaranteed to be a Node, as only Node instances can be part of the hierarchy.
 *
 * @return the nearest ancestor of this node that is an instance of klass.
 */
fun <T> Node.findAncestorOfType(klass: Class<T>): T? {
    return walkAncestors().filterIsInstance(klass).firstOrNull()
}
