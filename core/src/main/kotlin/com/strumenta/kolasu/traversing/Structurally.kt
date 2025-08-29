@file:JvmName("ProcessingStructurally")

package com.strumenta.kolasu.traversing

import com.strumenta.kolasu.model.ASTNode
import com.strumenta.kolasu.model.BaseASTNode
import com.strumenta.kolasu.model.Node
import java.util.ArrayDeque
import java.util.WeakHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction1

/**
 * Traverse the entire tree, deep first, starting from this Node.
 *
 * @return a Sequence representing the Nodes encountered.
 */
fun BaseASTNode.walk(): Sequence<BaseASTNode> {
    val stack: Stack<BaseASTNode> = mutableStackOf(this)
    return generateSequence {
        if (stack.isEmpty()) {
            null
        } else {
            val next: BaseASTNode = stack.pop()
            stack.pushAll(next.children)
            next
        }
    }
}

/**
 * Performs a post-order (or leaves-first) node traversal starting with a given node.
 */
fun BaseASTNode.walkLeavesFirst(): Sequence<BaseASTNode> {
    val nodesStack: Stack<List<BaseASTNode>> = mutableStackOf()
    val cursorStack: Stack<Int> = ArrayDeque()
    var done = false

    fun nextFromLevel(): BaseASTNode {
        val nodes: List<BaseASTNode> = nodesStack.peek()
        val cursor = cursorStack.pop()
        cursorStack.push(cursor + 1)
        return nodes[cursor]
    }

    fun fillStackToLeaf(node: BaseASTNode) {
        var currentNode: BaseASTNode = node
        while (true) {
            val childNodes: List<BaseASTNode> = currentNode.children
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
            val nodes: List<BaseASTNode> = nodesStack.peek()
            val cursor = cursorStack.peek()
            val levelHasNext = cursor < nodes.size
            if (levelHasNext) {
                val node: BaseASTNode = nodes[cursor]
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
fun BaseASTNode.walkAncestors(): Sequence<BaseASTNode> {
    var currentNode: BaseASTNode? = this
    return generateSequence {
        currentNode = currentNode!!.parent
        currentNode
    }
}

/**
 * @return all direct children of this node.
 */
fun BaseASTNode.walkChildren(includeDerived: Boolean = false): Sequence<BaseASTNode> =
    sequence {
        (
            if (includeDerived) {
                this@walkChildren.properties
            } else {
                this@walkChildren.originalProperties
            }
        ).forEach { property ->
            when (val value = property.value) {
                is BaseASTNode -> yield(value)
                is Collection<*> -> value.forEach { if (it is BaseASTNode) yield(it) }
            }
        }
    }

/**
 * @return all direct children of this node, together with the name of the containment of each child.
 */
fun ASTNode.walkChildrenByContainment(includeDerived: Boolean = false): Sequence<Pair<String, ASTNode>> =
    sequence {
        (
            if (includeDerived) {
                this@walkChildrenByContainment.properties
            } else {
                this@walkChildrenByContainment.originalProperties
            }
        ).forEach { property ->
            when (val value = property.value) {
                is Node -> yield(property.name to value)
                is Collection<*> -> value.forEach { if (it is Node) yield(property.name to it) }
            }
        }
    }

/**
 * @param walker a function that generates a sequence of nodes. By default this is the depth-first "walk" method.
 * For post-order traversal, take "walkLeavesFirst"
 * @return walks the whole AST starting from the childnodes of this node.
 */
@JvmOverloads
fun BaseASTNode.walkDescendants(
    walker: (BaseASTNode) ->
    Sequence<BaseASTNode> = BaseASTNode::walk,
): Sequence<BaseASTNode> = walker.invoke(this).filter { node -> node != this }

@JvmOverloads
fun <N : Any> BaseASTNode.walkDescendants(
    type: KClass<N>,
    walker: (BaseASTNode) -> Sequence<BaseASTNode> = BaseASTNode::walk,
): Sequence<N> = walkDescendants(walker).filterIsInstance(type.java)

/**
 * Note that type T is not strictly forced to be a Node. This is intended to support
 * interfaces like `Statement` or `Expression`. However, being an ancestor the returned
 * value is guaranteed to be a Node, as only Node instances can be part of the hierarchy.
 *
 * @return the nearest ancestor of this node that is an instance of klass.
 */
fun <T> BaseASTNode.findAncestorOfType(klass: Class<T>): T? = walkAncestors().filterIsInstance(klass).firstOrNull()

/**
 * @return all direct children of this node.
 */
val BaseASTNode.children: List<BaseASTNode>
    get() {
        return walkChildren().toList()
    }

/**
 * @return all direct children of this node.
 */
val Node.childrenByContainment: List<Pair<String, Node>>
    get() {
        return walkChildrenByContainment().toList()
    }

@JvmOverloads
fun <T> BaseASTNode.searchByType(
    klass: Class<T>,
    walker: KFunction1<BaseASTNode, Sequence<BaseASTNode>> = BaseASTNode::walk,
) = walker.invoke(this).filterIsInstance(klass)

/**
 * T is not forced to be a subtype of Node to support using interfaces.
 *
 * @param walker the function that generates the nodes to operate on in the desired sequence.
 * @return all nodes in this AST (sub)tree that are instances of, or extend [klass].
 */
fun <T> BaseASTNode.collectByType(
    klass: Class<T>,
    walker: KFunction1<BaseASTNode, Sequence<BaseASTNode>> = BaseASTNode::walk,
): List<T> = walker.invoke(this).filterIsInstance(klass).toList()

/**
 * The FastWalker is a walker that implements a cache to speed up subsequent walks.
 * The first walk will take the same time of a normal walk.
 * This walker will ignore any change to the nodes.
 */
class FastWalker(
    val node: BaseASTNode,
) {
    private val childrenMap: WeakHashMap<BaseASTNode, List<BaseASTNode>> = WeakHashMap<BaseASTNode, List<BaseASTNode>>()

    private fun getChildren(child: BaseASTNode): List<BaseASTNode> =
        if (childrenMap.containsKey(child)) {
            childrenMap[child]!!
        } else {
            childrenMap[child] = child.walkChildren().toList()
            childrenMap[child]!!
        }

    fun walk(root: BaseASTNode = node): Sequence<BaseASTNode> {
        val stack: Stack<BaseASTNode> = mutableStackOf(root)
        return generateSequence {
            if (stack.isEmpty()) {
                null
            } else {
                val next: BaseASTNode = stack.pop()
                stack.pushAll(getChildren(next))
                next
            }
        }
    }
}
