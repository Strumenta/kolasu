@file:JvmName("Processing")

package com.strumenta.kolasu.model

import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.Feature
import com.strumenta.kolasu.transformation.GenericNode
import com.strumenta.kolasu.traversing.ASTWalker
import com.strumenta.kolasu.traversing.children
import com.strumenta.kolasu.traversing.searchByType
import com.strumenta.kolasu.traversing.walk
import com.strumenta.kolasu.traversing.walkChildren
import java.util.IdentityHashMap
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Sets or corrects the parent of all AST nodes.
 * Kolasu does not see set/add/delete operations on the AST nodes,
 * so this function should be called manually after modifying the AST.
 */
fun NodeLike.assignParents() {
    this.walkChildren().forEach {
        if (it == this) {
            throw java.lang.IllegalStateException("A node cannot be parent of itself: $this")
        }
        it.parent = this
        it.assignParents()
    }
}

/**
 * Recursively execute [operation] on [this] node, and all nodes below this node.
 * @param walker the function that generates the nodes to operate on in the desired sequence.
 */
fun NodeLike.processNodes(
    operation: (NodeLike) -> Unit,
    walker: ASTWalker = NodeLike::walk,
) {
    walker.invoke(this).forEach(operation)
}

fun NodeLike.invalidRanges(): Sequence<NodeLike> =
    this.walk().filter {
        it.range == null ||
            run {
                val parentPos = it.parent?.range
                // If the parent range is null, we can't say anything about this node's range
                (parentPos != null && !(parentPos.contains(it.range!!.start) && parentPos.contains(it.range!!.end)))
            }
    }

fun NodeLike.findInvalidRange(): NodeLike? = this.invalidRanges().firstOrNull()

fun NodeLike.hasValidParents(parent: NodeLike? = this.parent): Boolean {
    return this.parent == parent && this.children.all { it.hasValidParents(this) }
}

fun <T : NodeLike> T.withParent(parent: NodeLike?): T {
    this.parent = parent
    return this
}

/**
 * Executes an operation on the properties of a node.
 * @param propertiesToIgnore which properties to ignore
 * @param propertyOperation the operation to perform on each property.
 */
@JvmOverloads
fun NodeLike.processFeatures(
    propertiesToIgnore: Set<String> = emptySet(),
    propertyOperation: (Feature, NodeLike) -> Unit,
) {
    if (this is GenericNode) {
        return
    }
    this.concept.declaredFeatures.filter { it.name !in propertiesToIgnore }.forEach {
        try {
            propertyOperation(it, this)
        } catch (t: Throwable) {
            throw java.lang.RuntimeException("Issue processing property $it in $this", t)
        }
    }
}

/**
 * Executes an operation on the properties of a node.
 * @param featuresToIgnore which properties to ignore
 * @param featureHandler the operation to perform on each property.
 */
@JvmOverloads
fun NodeLike.processFeatures(
    featuresToIgnore: Set<String> = emptySet(),
    featureHandler: (Feature) -> Unit,
) {
    this.concept.declaredFeatures.filter { it.name !in featuresToIgnore }.forEach {
        try {
            featureHandler(it)
        } catch (t: Throwable) {
            throw java.lang.RuntimeException("Issue processing property $it in $this", t)
        }
    }
}

/**
 * Executes an operation on the properties definitions of a node class.
 * @param propertiesToIgnore which properties to ignore
 * @param propertyTypeOperation the operation to perform on each property.
 */
fun <T : Any> Class<T>.processFeatures(
    propertiesToIgnore: Set<String> = emptySet(),
    propertyTypeOperation: (PropertyTypeDescription) -> Unit,
) = kotlin.processFeatures(propertiesToIgnore, propertyTypeOperation)

/**
 * @param walker the function that generates the nodes to operate on in the desired sequence.
 * @return the first node in the AST for which the [predicate] is true. Null if none are found.
 */
fun NodeLike.find(
    predicate: (NodeLike) -> Boolean,
    walker: ASTWalker = NodeLike::walk,
): NodeLike? {
    return walker.invoke(this).find(predicate)
}

/**
 * Recursively execute [operation] on this node, and all nodes below this node that extend [klass].
 *
 * T is not forced to be a subtype of Node to support using interfaces.
 *
 * @param walker the function that generates the nodes to operate on in the desired sequence.
 */
fun <T> NodeLike.processNodesOfType(
    klass: Class<T>,
    operation: (T) -> Unit,
    walker: ASTWalker = NodeLike::walk,
) {
    searchByType(klass, walker).forEach(operation)
}

/**
 * @return all direct children of this node.
 */
val NodeLike.children: List<NodeLike>
    get() {
        val children = mutableListOf<NodeLike>()
        this.concept.allContainments.filter { !it.derived }.forEach { containment ->
            children.addAll(this.getChildren(containment))
        }
        return children
    }

/**
 * @return the next sibling node. Notice that children of a sibling collection are considered siblings
 * and not the collection itself.
 */
val NodeLike.nextSibling: NodeLike?
    get() {
        if (this.parent != null) {
            val siblings = this.parent!!.children
            val index = siblings.indexOf(this)
            return if (index == siblings.size - 1) null else siblings[index + 1]
        }
        return null
    }

/**
 * @return the previous sibling. Notice that children of a sibling collection are considered siblings
 * and not the collection itself.
 */
val NodeLike.previousSibling: NodeLike?
    get() {
        if (this.parent != null) {
            val siblings = this.parent!!.children
            val index = siblings.indexOf(this)
            return if (index == 0) null else siblings[index - 1]
        }
        return null
    }

/**
 * @return the next sibling node in the same property.
 */
val NodeLike.nextSamePropertySibling: NodeLike?
    get() {
        if (this.parent != null) {
            val containment = this.containingContainment() ?: throw IllegalStateException()
            val siblingsAndMe = this.parent!!.getChildren(containment)
            val index = this.indexInContainingProperty() ?: throw IllegalStateException()
            return if (index == siblingsAndMe.size - 1) {
                null
            } else {
                siblingsAndMe[index + 1]
            }
        }
        return null
    }

/**
 * @return the previous sibling in the same property.
 */
val NodeLike.previousSamePropertySibling: NodeLike?
    get() {
        if (this.parent != null) {
            val containment = this.containingContainment() ?: throw IllegalStateException()
            val siblingsAndMe = this.parent!!.getChildren(containment)
            val index = this.indexInContainingProperty() ?: throw IllegalStateException()
            return if (index == 0) {
                null
            } else {
                siblingsAndMe[index - 1]
            }
        }
        return null
    }

/**
 * Return the property containing this Node, if any. Null should be returned for root nodes.
 */
fun NodeLike.containingContainment(): Containment? {
    if (this.parent == null) {
        return null
    }
    return this.parent!!.containments.find { c ->
        c.contained(this.parent!!).any { it === this }
    } ?: throw IllegalStateException("No containing feature for $this with parent ${this.parent}")
}

/**
 * Return the index of this Node within the containing property. The return value is null for root nodes.
 * The index is always 0 for Nodes in singular containment properties.
 */
fun NodeLike.indexInContainingProperty(): Int? {
    val containment = this.containingContainment() ?: return null
    val children = this.parent!!.getChildren(containment)
    return children.indexOfFirst { this === it }
}

/**
 * @return the next sibling of the specified type. Notice that children of a sibling collection are considered siblings
 * and not the collection itself.
 */
inline fun <reified T : NodeLike> NodeLike.nextSibling(): NodeLike? {
    if (this.parent != null) {
        val siblings = this.parent!!.children
        return siblings.takeLast(siblings.size - 1 - siblings.indexOf(this)).firstOrNull { it is T }
    }
    return null
}

/**
 * @return the previous sibling of the specified type. Notice that children of a sibling collection are considered siblings
 * and not the collection itself.
 */
inline fun <reified T : NodeLike> NodeLike.previousSibling(): NodeLike? {
    if (this.parent != null) {
        val siblings = this.parent!!.children
        return siblings.take(siblings.indexOf(this)).lastOrNull { it is T }
    }
    return null
}

// TODO reimplement using transformChildren
fun NodeLike.transformTree(
    operation: (NodeLike) -> NodeLike,
    inPlace: Boolean = false,
    mutationsCache: IdentityHashMap<NodeLike, NodeLike> = IdentityHashMap<NodeLike, NodeLike>(),
): NodeLike {
    if (inPlace) TODO()
    mutationsCache.computeIfAbsent(this) { operation(this) }
    val changes = mutableMapOf<String, Any>()
    relevantMemberProperties().forEach { p ->
        when (val v = p.get(this)) {
            is NodeLike -> {
                val newValue = v.transformTree(operation, inPlace, mutationsCache)
                if (newValue != v) changes[p.name] = newValue
            }

            is Collection<*> -> {
                val newValue =
                    v.map {
                        if (it is NodeLike) {
                            it.transformTree(
                                operation,
                                inPlace,
                                mutationsCache,
                            )
                        } else {
                            it
                        }
                    }
                if (newValue != v) changes[p.name] = newValue
            }
        }
    }
    var instanceToTransform = this
    if (changes.isNotEmpty()) {
        val constructor = this.javaClass.kotlin.primaryConstructor!!
        val params = mutableMapOf<KParameter, Any?>()
        constructor.parameters.forEach { param ->
            if (changes.containsKey(param.name)) {
                params[param] = changes[param.name]
            } else {
                params[param] =
                    this
                        .javaClass
                        .kotlin
                        .memberProperties
                        .find { param.name == it.name }!!
                        .get(this)
            }
        }
        instanceToTransform = constructor.callBy(params)
    }
    return mutationsCache.computeIfAbsent(instanceToTransform) { operation(instanceToTransform) }
}

class ImmutablePropertyException(
    property: KProperty<*>,
    node: NodeLike,
) : RuntimeException("Cannot mutate property '${property.name}' of node $node (class: ${node.javaClass.canonicalName})")

// assumption: every MutableList in the AST contains Nodes.
@Suppress("UNCHECKED_CAST")
fun NodeLike.transformChildren(operation: (NodeLike) -> NodeLike) {
    nodeProperties.forEach { property ->
        when (val value = property.get(this)) {
            is NodeLike -> {
                val newValue = operation(value)
                if (newValue != value) {
                    if (property is KMutableProperty<*>) {
                        property.setter.call(this, newValue)
                        newValue.parent = this
                    } else {
                        throw ImmutablePropertyException(property, this)
                    }
                }
            }

            is Collection<*> -> {
                if (value is List<*>) {
                    for (i in 0 until value.size) {
                        val element = value[i]
                        if (element is NodeLike) {
                            val newValue = operation(element)
                            if (newValue != element) {
                                if (value is MutableList<*>) {
                                    (value as MutableList<NodeLike>)[i] = newValue
                                    newValue.parent = this
                                } else {
                                    throw ImmutablePropertyException(property, element)
                                }
                            }
                        }
                    }
                } else {
                    throw UnsupportedOperationException(
                        "Only modifications in a List and MutableList are supported, not ${value::class}",
                    )
                }
            }
        }
    }
}

fun NodeLike.mapChildren(operation: (NodeLike) -> NodeLike): NodeLike {
    val changes = mutableMapOf<String, Any>()
    relevantMemberProperties().forEach { property ->
        when (val value = property.get(this)) {
            is NodeLike -> {
                val newValue = operation(value)
                if (newValue != value) {
                    changes[property.name] = newValue
                }
            }

            is Collection<*> -> {
                val newValue = value.map { if (it is NodeLike) operation(it) else it }
                if (newValue != value) {
                    changes[property.name] = newValue
                }
            }
        }
    }
    var instanceToTransform = this
    if (changes.isNotEmpty()) {
        val constructor = this.javaClass.kotlin.primaryConstructor!!
        val params = mutableMapOf<KParameter, Any?>()
        constructor.parameters.forEach { param ->
            if (changes.containsKey(param.name)) {
                params[param] = changes[param.name]
            } else {
                params[param] =
                    this
                        .javaClass
                        .kotlin
                        .memberProperties
                        .find { param.name == it.name }!!
                        .get(this)
            }
        }
        instanceToTransform = constructor.callBy(params)
    }
    return instanceToTransform
}

/**
 * Replace [this] node with [other] (by modifying the children of the parent node.)
 * For this to work, [NodeLike.assignParents] must have been called.
 *
 * Note that we recognize the exact same Node, looking at its identity, not using
 * equality.
 */
fun NodeLike.replaceWith(other: NodeLike) {
    if (this.parent == null) {
        throw IllegalStateException("Parent not set")
    }
    this.parent!!.transformChildren { if (it === this) other else it }
}

/**
 * Looks for [oldNode] in the lists of nodes in this node.
 * When found, it is removed, and in its place the [newNodes] are inserted.
 * When not found, an [IllegalStateException] is thrown.
 */
fun NodeLike.replaceWithSeveral(
    oldNode: NodeLike,
    newNodes: List<NodeLike>,
) {
    findMutableListContainingChild(oldNode) { nodeList, index ->
        nodeList.replaceWithSeveral(index, newNodes)
        oldNode.parent = null
        newNodes.forEach { node -> node.parent = this }
    }
}

/**
 * Looks for [targetNode] in the lists of nodes in this node.
 * When found, it is removed.
 * When not found, an [IllegalStateException] is thrown.
 */
fun NodeLike.removeFromList(targetNode: NodeLike) {
    findMutableListContainingChild(targetNode) { nodeList, index ->
        nodeList.removeAt(index)
        targetNode.parent = null
    }
}

/**
 * Looks for [targetNode] in the lists of nodes in this node.
 * When found, [newNodes] are inserted before it.
 * When not found, an [IllegalStateException] is thrown.
 */
fun NodeLike.addSeveralBefore(
    targetNode: NodeLike,
    newNodes: List<NodeLike>,
) {
    findMutableListContainingChild(targetNode) { nodeList, index ->
        nodeList.addSeveralBefore(index, newNodes)
        newNodes.forEach { node -> node.parent = this }
    }
}

/**
 * Looks for [targetNode] in the lists of nodes in this node.
 * When found, [newNodes] are inserted after it.
 * When not found, an [IllegalStateException] is thrown.
 */
fun NodeLike.addSeveralAfter(
    targetNode: NodeLike,
    newNodes: List<NodeLike>,
) {
    findMutableListContainingChild(targetNode) { nodeList, index ->
        nodeList.addSeveralAfter(index, newNodes)
        newNodes.forEach { node -> node.parent = this }
    }
}

/**
 * Supports functions that manipulate a list of child nodes by finding [targetNode] in the [MutableList]s of nodes contained in [this] node.
 */
@Suppress("UNCHECKED_CAST") // assumption: a MutableList with a Node in it is a MutableList<Node>
private fun NodeLike.findMutableListContainingChild(
    targetNode: NodeLike,
    whenFoundDo: (nodeList: MutableList<NodeLike>, index: Int) -> Unit,
) {
    relevantMemberProperties().forEach { property ->
        when (val value = property.get(this)) {
            is MutableList<*> -> {
                for (i in 0 until value.size) {
                    // We want to find a particular child, not just one which is equal to it
                    if (value[i] === targetNode) {
                        whenFoundDo(value as MutableList<NodeLike>, i)
                        return
                    }
                }
            }
        }
    }
    throw IllegalStateException("Did not find $targetNode in any MutableList in $this.")
}

/**
 * Replaces [this] node with any amount of other nodes if it is in a [MutableList].
 * <p/>Looks for [this] in the lists of nodes in the parent node.
 * When found, [this] is removed, and in its place [newNodes] are inserted.
 * For this to work, [NodeLike.assignParents] must have been called.
 */
fun NodeLike.replaceWithSeveral(newNodes: List<NodeLike>) {
    parent?.replaceWithSeveral(this, newNodes) ?: throw IllegalStateException("Parent not set")
}

/**
 * Inserts the [newNodes] before [this] node if it is in a [MutableList].
 * For this to work, [NodeLike.assignParents] must have been called.
 */
fun NodeLike.addSeveralBefore(newNodes: List<NodeLike>) {
    parent?.addSeveralBefore(this, newNodes) ?: throw IllegalStateException("Parent not set")
}

/**
 * Inserts the [newNodes] after [this] node if it is in a [MutableList].
 * For this to work, [NodeLike.assignParents] must have been called.
 */
fun NodeLike.addSeveralAfter(newNodes: List<NodeLike>) {
    parent?.addSeveralAfter(this, newNodes) ?: throw IllegalStateException("Parent not set")
}

/**
 * Removes [this] node from the parent if it is in a [MutableList].
 * For this to work, [NodeLike.assignParents] must have been called.
 */
fun NodeLike.removeFromList() {
    parent?.removeFromList(this) ?: throw IllegalStateException("Parent not set")
}

/**
 * Replaces the element at [index] with [replacements].
 */
fun <T> MutableList<T>.replaceWithSeveral(
    index: Int,
    replacements: List<T>,
) {
    removeAt(index)
    addAll(index, replacements)
}

/**
 * Replaces the element at [index] with [additions].
 */
fun <T> MutableList<T>.addSeveralBefore(
    index: Int,
    additions: List<T>,
) {
    addAll(index, additions)
}

/**
 * Replaces the element at [index] with [additions].
 */
fun <T> MutableList<T>.addSeveralAfter(
    index: Int,
    additions: List<T>,
) {
    addAll(index + 1, additions)
}
