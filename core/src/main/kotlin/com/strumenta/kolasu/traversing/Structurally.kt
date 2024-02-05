@file:JvmName("ProcessingStructurally")

package com.strumenta.kolasu.traversing

import com.strumenta.kolasu.model.Node
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction1

/**
 * Traverse the entire tree, deep first, starting from this Node.
 *
 * @return a Sequence representing the Nodes encountered.
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
fun Node.walkChildren(includeDerived: Boolean = false): Sequence<Node> {
    return sequence {
        (
            if (includeDerived) {
                this@walkChildren.properties
            } else {
                this@walkChildren.originalProperties
            }
            ).forEach { property ->
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

/**
 * @return all direct children of this node.
 */
val Node.children: List<Node>
    get() {
        return walkChildren().toList()
    }

@JvmOverloads
fun <T> Node.searchByType(
    klass: Class<T>,
    walker: KFunction1<Node, Sequence<Node>> = Node::walk
) = walker.invoke(this).filterIsInstance(klass)

/**
 * T is not forced to be a subtype of Node to support using interfaces.
 *
 * @param walker the function that generates the nodes to operate on in the desired sequence.
 * @return all nodes in this AST (sub)tree that are instances of, or extend [klass].
 */
fun <T> Node.collectByType(klass: Class<T>, walker: KFunction1<Node, Sequence<Node>> = Node::walk): List<T> {
    return walker.invoke(this).filterIsInstance(klass).toList()
}

/**
 * The FastWalker is a walker that implements a cache to speed up subsequent walks.
 * The first walk will take the same time of a normal walk.
 * This walker will ignore any change to the nodes.
 */
class FastWalker(val node: Node) {
    private val childrenMap: WeakHashMap<Node, List<Node>> = WeakHashMap<Node, List<Node>>()

    private fun getChildren(child: Node): List<Node> {
        return if (childrenMap.containsKey(child)) {
            childrenMap[child]!!
        } else {
            childrenMap.put(child, child.walkChildren().toList())
            childrenMap[child]!!
        }
    }

    fun walk(root: Node = node): Sequence<Node> {
        val stack: Stack<Node> = mutableStackOf(root)
        return generateSequence {
            if (stack.isEmpty()) {
                null
            } else {
                val next: Node = stack.pop()
                stack.pushAll(getChildren(next))
                next
            }
        }
    }
}
