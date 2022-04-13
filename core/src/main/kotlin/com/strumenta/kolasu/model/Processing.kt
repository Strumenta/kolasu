@file:JvmName("Processing")
package com.strumenta.kolasu.model

import java.util.*
import kotlin.reflect.*
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Sets or corrects the parent of all AST nodes.
 * Kolasu does not see set/add/delete operations on the AST nodes,
 * so this function should be called manually after modifying the AST.
 */
fun Node.assignParents() {
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
fun Node.processNodes(operation: (Node) -> Unit, walker: KFunction1<Node, Sequence<Node>> = Node::walk) {
    walker.invoke(this).forEach(operation)
}

fun Node.invalidPositions(): Sequence<Node> = this.walk().filter {
    it.position == null || run {
        val parentPos = it.parent?.position
        // If the parent position is null, we can't say anything about this node's position
        (parentPos != null && !(parentPos.contains(it.position!!.start) && parentPos.contains(it.position!!.end)))
    }
}

fun Node.findInvalidPosition(): Node? = this.invalidPositions().firstOrNull()

fun <T : Node> T.withParent(parent: Node?): T {
    this.parent = parent
    return this
}

private fun providesNodes(kTypeProjection: KTypeProjection): Boolean {
    val ktype = kTypeProjection.type
    return when (ktype) {
        is KClass<*> -> providesNodes(ktype as? KClass<*>)
        is KType -> providesNodes((ktype as? KType)?.classifier)
        else -> throw UnsupportedOperationException(
            "We are not able to determine if the type $ktype provides AST Nodes or not"
        )
    }
}

private fun providesNodes(classifier: KClassifier?): Boolean {
    if (classifier == null) {
        return false
    }
    if (classifier is KClass<*>) {
        return providesNodes(classifier as? KClass<*>)
    } else {
        throw UnsupportedOperationException(
            "We are not able to determine if the classifier $classifier provides AST Nodes or not"
        )
    }
}

private fun providesNodes(kclass: KClass<*>?): Boolean {
    return kclass?.isANode() ?: false
}

/**
 * @return can [this] class be considered an AST node?
 */
fun KClass<*>.isANode(): Boolean {
    return this.isSubclassOf(Node::class) || this.isMarkedAsNodeType()
}

/**
 * @return is [this] class annotated with NodeType?
 */
fun KClass<*>.isMarkedAsNodeType(): Boolean {
    return this.annotations.any { it.annotationClass == NodeType::class }
}

data class PropertyTypeDescription(
    val name: String,
    val provideNodes: Boolean,
    val multiple: Boolean,
    val valueType: KType
) {
    companion object {
        fun buildFor(property: KProperty1<*, *>): PropertyTypeDescription {
            val propertyType = property.returnType
            val classifier = propertyType.classifier as? KClass<*>
            val multiple = (classifier?.isSubclassOf(Collection::class) == true)
            var valueType: KType
            val provideNodes = if (multiple) {
                valueType = propertyType.arguments[0].type!!
                providesNodes(propertyType.arguments[0])
            } else {
                valueType = propertyType
                providesNodes(classifier)
            }
            return PropertyTypeDescription(
                name = property.name,
                provideNodes = provideNodes,
                multiple = multiple,
                valueType = valueType
            )
        }
    }
}

enum class Multeplicity {
    OPTIONAL,
    SINGULAR,
    MANY
}

data class PropertyDescription(
    val name: String,
    val provideNodes: Boolean,
    val multeplicity: Multeplicity,
    val value: Any?
) {

    val multiple: Boolean
        get() = multeplicity == Multeplicity.MANY

    companion object {

        fun multiple(property: KProperty1<in Node, *>): Boolean {
            val propertyType = property.returnType
            val classifier = propertyType.classifier as? KClass<*>
            return (classifier?.isSubclassOf(Collection::class) == true)
        }

        fun optional(property: KProperty1<in Node, *>): Boolean {
            val propertyType = property.returnType
            return !multiple(property) && propertyType.isMarkedNullable
        }

        fun multeplicity(property: KProperty1<in Node, *>): Multeplicity {
            return when {
                multiple(property) -> Multeplicity.MANY
                optional(property) -> Multeplicity.OPTIONAL
                else -> Multeplicity.SINGULAR
            }
        }

        fun providesNodes(property: KProperty1<in Node, *>): Boolean {
            val propertyType = property.returnType
            val classifier = propertyType.classifier as? KClass<*>
            return if (multiple(property)) {
                providesNodes(propertyType.arguments[0])
            } else {
                providesNodes(classifier)
            }
        }

        fun buildFor(property: KProperty1<in Node, *>, node: Node): PropertyDescription {
            val multeplicity = multeplicity(property)
            val provideNodes = providesNodes(property)
            return PropertyDescription(
                name = property.name,
                provideNodes = provideNodes,
                multeplicity = multeplicity,
                value = property.get(node)
            )
        }
    }
}

val DEFAULT_IGNORED_PROPERTIES = setOf("nodeType", "origin", "position")

/**
 * Executes an operation on the properties of a node.
 * @param propertiesToIgnore which properties to ignore. @see DEFAULT_IGNORED_PROPERTIES
 * @param propertyOperation the operation to perform on each property.
 */
@JvmOverloads
fun Node.processProperties(
    propertiesToIgnore: Set<String> = DEFAULT_IGNORED_PROPERTIES,
    propertyOperation: (PropertyDescription) -> Unit
) {
    this.properties.filter { it.name !in propertiesToIgnore }.forEach {
        try {
            propertyOperation(it)
        } catch (t: Throwable) {
            throw java.lang.RuntimeException("Issue processing property $it in $this", t)
        }
    }
}

/**
 * Executes an operation on the properties definitions of a node class.
 * @param propertiesToIgnore which properties to ignore. @see DEFAULT_IGNORED_PROPERTIES
 * @param propertyTypeOperation the operation to perform on each property.
 */
fun <T : Any> Class<T>.processProperties(
    propertiesToIgnore: Set<String> = DEFAULT_IGNORED_PROPERTIES,
    propertyTypeOperation: (PropertyTypeDescription) -> Unit
) = kotlin.processProperties(propertiesToIgnore, propertyTypeOperation)

/**
 * Executes an operation on the properties definitions of a node class.
 * @param propertiesToIgnore which properties to ignore. @see DEFAULT_IGNORED_PROPERTIES
 * @param propertyTypeOperation the operation to perform on each property.
 */
fun <T : Any> KClass<T>.processProperties(
    propertiesToIgnore: Set<String> = DEFAULT_IGNORED_PROPERTIES,
    propertyTypeOperation: (PropertyTypeDescription) -> Unit
) {
    nodeProperties.forEach { p ->
        if (!propertiesToIgnore.contains(p.name)) {
            propertyTypeOperation(PropertyTypeDescription.buildFor(p))
        }
    }
}

/**
 * @param walker the function that generates the nodes to operate on in the desired sequence.
 * @return the first node in the AST for which the [predicate] is true. Null if none are found.
 */
fun Node.find(predicate: (Node) -> Boolean, walker: KFunction1<Node, Sequence<Node>> = Node::walk): Node? {
    return walker.invoke(this).find(predicate)
}

/**
 * Recursively execute [operation] on this node, and all nodes below this node that extend [klass].
 *
 * T is not forced to be a subtype of Node to support using interfaces.
 *
 * @param walker the function that generates the nodes to operate on in the desired sequence.
 */
fun <T> Node.processNodesOfType(
    klass: Class<T>,
    operation: (T) -> Unit,
    walker: KFunction1<Node, Sequence<Node>> = Node::walk
) {
    walker.invoke(this).filterIsInstance(klass).forEach(operation)
}

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
 * Recursively execute [operation] on this node, and all nodes below this node.
 * Every node is informed about its [parent] node. (But not about the parent's parent!)
 */
fun Node.processConsideringDirectParent(operation: (Node, Node?) -> Unit, parent: Node? = null) {
    operation(this, parent)
    this.properties.forEach { p ->
        when (val v = p.value) {
            is Node -> v.processConsideringDirectParent(operation, this)
            is Collection<*> -> v.forEach { (it as? Node)?.processConsideringDirectParent(operation, this) }
        }
    }
}

/**
 * @return all direct children of this node.
 */
val Node.children: List<Node>
    get() {
        val children = mutableListOf<Node>()
        this.properties.forEach { p ->
            val v = p.value
            when (v) {
                is Node -> children.add(v)
                is Collection<*> -> v.forEach { if (it is Node) children.add(it) }
            }
        }
        return children
    }

// TODO reimplement using transformChildren
fun Node.transformTree(
    operation: (Node) -> Node,
    inPlace: Boolean = false,
    mutationsCache: IdentityHashMap<Node, Node> = IdentityHashMap<Node, Node>()
): Node {
    if (inPlace) TODO()
    mutationsCache.computeIfAbsent(this) { operation(this) }
    val changes = mutableMapOf<String, Any>()
    relevantMemberProperties().forEach { p ->
        val v = p.get(this)
        when (v) {
            is Node -> {
                val newValue = v.transformTree(operation, inPlace, mutationsCache)
                if (newValue != v) changes[p.name] = newValue
            }
            is Collection<*> -> {
                val newValue = v.map { if (it is Node) it.transformTree(operation, inPlace, mutationsCache) else it }
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
                params[param] = this.javaClass.kotlin.memberProperties.find { param.name == it.name }!!.get(this)
            }
        }
        instanceToTransform = constructor.callBy(params)
    }
    return mutationsCache.computeIfAbsent(instanceToTransform) { operation(instanceToTransform) }
}

class ImmutablePropertyException(property: KProperty<*>, node: Node) :
    RuntimeException("Cannot mutate property '${property.name}' of node $node (class: ${node.javaClass.canonicalName})")

// assumption: every MutableList in the AST contains Nodes.
@Suppress("UNCHECKED_CAST")
fun Node.transformChildren(operation: (Node) -> Node) {
    relevantMemberProperties().forEach { property ->
        val value = property.get(this)
        when (value) {
            is Node -> {
                val newValue = operation(value)
                if (newValue != value) {
                    if (property is KMutableProperty<*>) {
                        property.setter.call(this, newValue)
                        newValue.parent = this
                    } else {
                        throw ImmutablePropertyException(property, value)
                    }
                }
            }
            is Collection<*> -> {
                if (value is List<*>) {
                    for (i in 0 until value.size) {
                        val element = value[i]
                        if (element is Node) {
                            val newValue = operation(element)
                            if (newValue != element) {
                                if (value is MutableList<*>) {
                                    (value as MutableList<Node>)[i] = newValue
                                    newValue.parent = this
                                } else {
                                    throw ImmutablePropertyException(property, element)
                                }
                            }
                        }
                    }
                } else {
                    throw UnsupportedOperationException(
                        "Only modifications in a List and MutableList are supported, not ${value::class}"
                    )
                }
            }
        }
    }
}

fun Node.mapChildren(operation: (Node) -> Node): Node {
    val changes = mutableMapOf<String, Any>()
    relevantMemberProperties().forEach { property ->
        val value = property.get(this)
        when (value) {
            is Node -> {
                val newValue = operation(value)
                if (newValue != value) {
                    changes[property.name] = newValue
                }
            }
            is Collection<*> -> {
                val newValue = value.map { if (it is Node) operation(it) else it }
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
                params[param] = this.javaClass.kotlin.memberProperties.find { param.name == it.name }!!.get(this)
            }
        }
        instanceToTransform = constructor.callBy(params)
    }
    return instanceToTransform
}

/**
 * Replace [this] node with [other] (by modifying the children of the parent node.)
 * For this to work, [Node.assignParents] must have been called.
 */
fun Node.replaceWith(other: Node) {
    if (this.parent == null) {
        throw IllegalStateException("Parent not set")
    }
    this.parent!!.transformChildren { if (it == this) other else it }
}

/**
 * Looks for [oldNode] in the lists of nodes in this node.
 * When found, it is removed, and in its place the [newNodes] are inserted.
 * When not found, an [IllegalStateException] is thrown.
 */
fun Node.replaceWithSeveral(oldNode: Node, newNodes: List<Node>) {
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
fun Node.removeFromList(targetNode: Node) {
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
fun Node.addSeveralBefore(targetNode: Node, newNodes: List<Node>) {
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
fun Node.addSeveralAfter(targetNode: Node, newNodes: List<Node>) {
    findMutableListContainingChild(targetNode) { nodeList, index ->
        nodeList.addSeveralAfter(index, newNodes)
        newNodes.forEach { node -> node.parent = this }
    }
}

/**
 * Supports functions that manipulate a list of child nodes by finding [targetNode] in the [MutableList]s of nodes contained in [this] node.
 */
@Suppress("UNCHECKED_CAST") // assumption: a MutableList with a Node in it is a MutableList<Node>
private fun Node.findMutableListContainingChild(
    targetNode: Node,
    whenFoundDo: (nodeList: MutableList<Node>, index: Int) -> Unit
) {
    relevantMemberProperties().forEach { property ->
        when (val value = property.get(this)) {
            is MutableList<*> -> {
                for (i in 0 until value.size) {
                    if (value[i] == targetNode) {
                        whenFoundDo(value as MutableList<Node>, i)
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
 * For this to work, [Node.assignParents] must have been called.
 */
fun Node.replaceWithSeveral(newNodes: List<Node>) {
    parent?.replaceWithSeveral(this, newNodes) ?: throw IllegalStateException("Parent not set")
}

/**
 * Inserts the [newNodes] before [this] node if it is in a [MutableList].
 * For this to work, [Node.assignParents] must have been called.
 */
fun Node.addSeveralBefore(newNodes: List<Node>) {
    parent?.addSeveralBefore(this, newNodes) ?: throw IllegalStateException("Parent not set")
}

/**
 * Inserts the [newNodes] after [this] node if it is in a [MutableList].
 * For this to work, [Node.assignParents] must have been called.
 */
fun Node.addSeveralAfter(newNodes: List<Node>) {
    parent?.addSeveralAfter(this, newNodes) ?: throw IllegalStateException("Parent not set")
}

/**
 * Removes [this] node from the parent if it is in a [MutableList].
 * For this to work, [Node.assignParents] must have been called.
 */
fun Node.removeFromList() {
    parent?.removeFromList(this) ?: throw IllegalStateException("Parent not set")
}

/**
 * Replaces the element at [index] with [replacements].
 */
fun <T> MutableList<T>.replaceWithSeveral(index: Int, replacements: List<T>) {
    removeAt(index)
    addAll(index, replacements)
}

/**
 * Replaces the element at [index] with [additions].
 */
fun <T> MutableList<T>.addSeveralBefore(index: Int, additions: List<T>) {
    addAll(index, additions)
}

/**
 * Replaces the element at [index] with [additions].
 */
fun <T> MutableList<T>.addSeveralAfter(index: Int, additions: List<T>) {
    addAll(index + 1, additions)
}
