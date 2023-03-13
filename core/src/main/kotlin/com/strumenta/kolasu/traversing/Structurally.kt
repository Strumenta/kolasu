@file:JvmName("ProcessingStructurally")
package com.strumenta.kolasu.traversing

import com.strumenta.kolasu.model.ASTNode
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction1

/**
 * Traverse the entire tree, deep first, starting from this Node.
 *
 * @return a Sequence representing the Nodes encountered.
 */
fun ASTNode.walk(): Sequence<ASTNode> {
    val stack: Stack<ASTNode> = mutableStackOf(this)
    return generateSequence {
        if (stack.isEmpty()) {
            null
        } else {
            val next: ASTNode = stack.pop()
            stack.pushAll(next.children)
            next
        }
    }
}

/**
 * Performs a post-order (or leaves-first) node traversal starting with a given node.
 */
fun ASTNode.walkLeavesFirst(): Sequence<ASTNode> {
    val nodesStack: Stack<List<ASTNode>> = mutableStackOf()
    val cursorStack: Stack<Int> = ArrayDeque()
    var done = false

    fun nextFromLevel(): ASTNode {
        val nodes: List<ASTNode> = nodesStack.peek()
        val cursor = cursorStack.pop()
        cursorStack.push(cursor + 1)
        return nodes[cursor]
    }

    fun fillStackToLeaf(node: ASTNode) {
        var currentNode: ASTNode = node
        while (true) {
            val childNodes: List<ASTNode> = currentNode.children
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
            val nodes: List<ASTNode> = nodesStack.peek()
            val cursor = cursorStack.peek()
            val levelHasNext = cursor < nodes.size
            if (levelHasNext) {
                val node: ASTNode = nodes[cursor]
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
fun ASTNode.walkAncestors(): Sequence<ASTNode> {
    var currentNode: ASTNode? = this
    return generateSequence {
        currentNode = currentNode!!.parent
        currentNode
    }
}

/**
 * @return all direct children of this node.
 */
fun ASTNode.walkChildren(): Sequence<ASTNode> {
    return sequence {
        this@walkChildren.properties.forEach { property ->
            when (val value = property.value) {
                is ASTNode -> yield(value)
                is Collection<*> -> value.forEach { if (it is ASTNode) yield(it) }
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
fun ASTNode.walkDescendants(walker: (ASTNode) -> Sequence<ASTNode> = ASTNode::walk): Sequence<ASTNode> {
    return walker.invoke(this).filter { node -> node != this }
}

@JvmOverloads
fun <N : Any> ASTNode.walkDescendants(type: KClass<N>, walker: (ASTNode) -> Sequence<ASTNode> = ASTNode::walk): Sequence<N> {
    return walkDescendants(walker).filterIsInstance(type.java)
}

/**
 * Note that type T is not strictly forced to be a Node. This is intended to support
 * interfaces like `Statement` or `Expression`. However, being an ancestor the returned
 * value is guaranteed to be a Node, as only Node instances can be part of the hierarchy.
 *
 * @return the nearest ancestor of this node that is an instance of klass.
 */
fun <T> ASTNode.findAncestorOfType(klass: Class<T>): T? {
    return walkAncestors().filterIsInstance(klass).firstOrNull()
}

/**
 * @return all direct children of this node.
 */
val ASTNode.children: List<ASTNode>
    get() {
        return walkChildren().toList()
    }

@JvmOverloads
fun <T> ASTNode.searchByType(
    klass: Class<T>,
    walker: KFunction1<ASTNode, Sequence<ASTNode>> = ASTNode::walk
) = walker.invoke(this).filterIsInstance(klass)

/**
 * T is not forced to be a subtype of Node to support using interfaces.
 *
 * @param walker the function that generates the nodes to operate on in the desired sequence.
 * @return all nodes in this AST (sub)tree that are instances of, or extend [klass].
 */
fun <T> ASTNode.collectByType(klass: Class<T>, walker: KFunction1<ASTNode, Sequence<ASTNode>> = ASTNode::walk): List<T> {
    return walker.invoke(this).filterIsInstance(klass).toList()
}
