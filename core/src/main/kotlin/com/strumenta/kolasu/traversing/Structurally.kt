@file:JvmName("ProcessingStructurally")

package com.strumenta.kolasu.traversing

import com.strumenta.kolasu.model.NodeLike
import java.util.ArrayDeque
import java.util.WeakHashMap
import kotlin.reflect.KClass

/**
 * Traverse the entire tree, deep first, starting from this Node.
 *
 * @return a Sequence representing the Nodes encountered.
 */
fun NodeLike.walk(): Sequence<NodeLike> {
    val stack: Stack<NodeLike> = mutableStackOf(this)
    return generateSequence {
        if (stack.isEmpty()) {
            null
        } else {
            val next: NodeLike = stack.pop()
            stack.pushAll(next.children)
            next
        }
    }
}

/**
 * Performs a post-order (or leaves-first) node traversal starting with a given node.
 */
fun NodeLike.walkLeavesFirst(): Sequence<NodeLike> {
    val nodesStack: Stack<List<NodeLike>> = mutableStackOf()
    val cursorStack: Stack<Int> = ArrayDeque()
    var done = false

    fun nextFromLevel(): NodeLike {
        val nodes: List<NodeLike> = nodesStack.peek()
        val cursor = cursorStack.pop()
        cursorStack.push(cursor + 1)
        return nodes[cursor]
    }

    fun fillStackToLeaf(node: NodeLike) {
        var currentNode: NodeLike = node
        while (true) {
            val childNodes: List<NodeLike> = currentNode.children
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
            val nodes: List<NodeLike> = nodesStack.peek()
            val cursor = cursorStack.peek()
            val levelHasNext = cursor < nodes.size
            if (levelHasNext) {
                val node: NodeLike = nodes[cursor]
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
fun NodeLike.walkAncestors(): Sequence<NodeLike> {
    var currentNode: NodeLike? = this
    return generateSequence {
        currentNode = currentNode!!.parent
        currentNode
    }
}

/**
 * @return all direct children of this node.
 */
fun NodeLike.walkChildren(): Sequence<NodeLike> {
    return sequence {
        this@walkChildren.properties.forEach { property ->
            when (val value = property.value) {
                is NodeLike -> yield(value)
                is Collection<*> -> value.forEach { if (it is NodeLike) yield(it) }
            }
        }
    }
}

typealias ASTWalker = (NodeLike) -> Sequence<NodeLike>

/**
 * @param walker a function that generates a sequence of nodes. By default this is the depth-first "walk" method.
 * For post-order traversal, take "walkLeavesFirst"
 * @return walks the whole AST starting from the childnodes of this node.
 */
@JvmOverloads
fun NodeLike.walkDescendants(walker: ASTWalker = NodeLike::walk): Sequence<NodeLike> {
    return walker.invoke(this).filter { node -> node != this }
}

@JvmOverloads
fun <N : Any> NodeLike.walkDescendants(
    type: KClass<N>,
    walker: ASTWalker = NodeLike::walk,
): Sequence<N> {
    return walkDescendants(walker).filterIsInstance(type.java)
}

/**
 * Note that type T is not strictly forced to be a Node. This is intended to support
 * interfaces like `Statement` or `Expression`. However, being an ancestor the returned
 * value is guaranteed to be a Node, as only Node instances can be part of the hierarchy.
 *
 * @return the nearest ancestor of this node that is an instance of klass.
 */
fun <T> NodeLike.findAncestorOfType(klass: Class<T>): T? {
    return walkAncestors().filterIsInstance(klass).firstOrNull()
}

/**
 * @return all direct children of this node.
 */
val NodeLike.children: List<NodeLike>
    get() {
        return walkChildren().toList()
    }

@JvmOverloads
fun <T> NodeLike.searchByType(
    klass: Class<T>,
    walker: ASTWalker = NodeLike::walk,
) = walker.invoke(this).filterIsInstance(klass)

/**
 * T is not forced to be a subtype of Node to support using interfaces.
 *
 * @param walker the function that generates the nodes to operate on in the desired sequence.
 * @return all nodes in this AST (sub)tree that are instances of, or extend [klass].
 */
fun <T> NodeLike.collectByType(
    klass: Class<T>,
    walker: ASTWalker = NodeLike::walk,
): List<T> {
    return walker.invoke(this).filterIsInstance(klass).toList()
}

/**
 * The FastWalker is a walker that implements a cache to speed up subsequent walks.
 * The first walk will take the same time of a normal walk.
 * This walker will ignore any change to the nodes.
 */
class FastWalker(
    val node: NodeLike,
) {
    private val childrenMap: WeakHashMap<NodeLike, List<NodeLike>> = WeakHashMap<NodeLike, List<NodeLike>>()

    private fun getChildren(child: NodeLike): List<NodeLike> {
        return if (childrenMap.containsKey(child)) {
            childrenMap[child]!!
        } else {
            childrenMap.put(child, child.walkChildren().toList())
            childrenMap[child]!!
        }
    }

    fun walk(root: NodeLike = node): Sequence<NodeLike> {
        val stack: Stack<NodeLike> = mutableStackOf(root)
        return generateSequence {
            if (stack.isEmpty()) {
                null
            } else {
                val next: NodeLike = stack.pop()
                stack.pushAll(getChildren(next))
                next
            }
        }
    }
}
