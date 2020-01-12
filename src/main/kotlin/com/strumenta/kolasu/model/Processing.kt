package com.strumenta.kolasu.model

import kotlin.reflect.*
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * @return all properties of this node that are considered AST properties.
 */
internal val <T : Node> T.containmentProperties: Collection<KProperty1<T, *>>
    get() = this.javaClass.kotlin.memberProperties
        .filter { it.visibility == KVisibility.PUBLIC }
        .filter { it.findAnnotation<Derived>() == null }
        .filter { it.findAnnotation<Link>() == null }
        .filter { it.name != "parent" }

/**
 * Sets or corrects the parent of all AST nodes.
 * Kolasu does not see set/add/delete operations on the AST nodes,
 * so this function should be called manually after modifying the AST.
 */
fun Node.assignParents() {
    this.walkChildren().forEach {
        it.parent = this
        it.assignParents()
    }
}

/**
 * Recursively execute "operation" on this node, and all nodes below this node.
 * @param walker the function that generates the nodes to operate on in the desired sequence.
 */
fun Node.processNodes(operation: (Node) -> Unit, walker: KFunction1<Node, Sequence<Node>> = Node::walk) {
    walker.invoke(this).forEach(operation)
}

private fun provideNodes(kTypeProjection: KTypeProjection): Boolean {
    val ktype = kTypeProjection.type
    return when (ktype) {
        is KClass<*> -> provideNodes(ktype as? KClass<*>)
        is KType -> provideNodes((ktype as? KType)?.classifier)
        else -> throw UnsupportedOperationException("We are not able to determine if the type $ktype provides AST Nodes or not")
    }
}

private fun provideNodes(classifier: KClassifier?): Boolean {
    if (classifier == null) {
        return false
    }
    if (classifier is KClass<*>) {
        return provideNodes(classifier as? KClass<*>)
    } else {
        throw UnsupportedOperationException("We are not able to determine if the classifier $classifier provides AST Nodes or not")
    }
}

private fun provideNodes(kclass: KClass<*>?): Boolean {
    return kclass?.representsNode() ?: false
}

/**
 * @return can this class be considered an AST node?
 */
fun KClass<*>.representsNode(): Boolean {
    return this.isSubclassOf(Node::class) || this.isMarkedAsNodeType()
}

/**
 * @return is this class annotated with NodeType?
 */
fun KClass<*>.isMarkedAsNodeType(): Boolean {
    return this.annotations.any { it.annotationClass == NodeType::class }
}

data class PropertyDescription(val name: String, val provideNodes: Boolean, val multiple: Boolean, val value: Any?) {
    companion object {
        fun buildFor(property: KProperty1<in Node, *>, node: Node): PropertyDescription {
            val propertyType = property.returnType
            val classifier = propertyType.classifier as? KClass<*>
            val multiple = (classifier?.isSubclassOf(Collection::class) == true)
            val provideNodes = if (multiple) {
                provideNodes(propertyType.arguments[0])
            } else {
                provideNodes(classifier)
            }
            return PropertyDescription(
                name = property.name,
                provideNodes = provideNodes,
                multiple = multiple,
                value = property.get(node)
            )
        }
    }
}

fun Node.processProperties(
    propertiesToIgnore: Set<String> = setOf("parseTreeNode", "position", "specifiedPosition"),
    propertyOperation: (PropertyDescription) -> Unit
) {
    containmentProperties.forEach { p ->
        if (!propertiesToIgnore.contains(p.name)) {
            propertyOperation(PropertyDescription.buildFor(p, this))
        }
    }
}

/**
 * @param walker the function that generates the nodes to operate on in the desired sequence.
 * @return the first node in the AST for which the predicate is true. Null if none are found.
 */
fun Node.find(predicate: (Node) -> Boolean, walker: KFunction1<Node, Sequence<Node>> = Node::walk): Node? {
    return walker.invoke(this).find(predicate)
}

/**
 * Recursively execute "operation" on this node, and all nodes below this node that extend klass.
 * @param walker the function that generates the nodes to operate on in the desired sequence.
 */
fun <T : Node> Node.specificProcess(klass: Class<T>, operation: (T) -> Unit, walker: KFunction1<Node, Sequence<Node>> = Node::walk) {
    walker.invoke(this).filterIsInstance(klass).forEach(operation)
}

/**
 * @param walker the function that generates the nodes to operate on in the desired sequence.
 * @return all nodes in this AST (sub)tree that are instances of, or extend klass.
 */
fun <T : Node> Node.collectByType(klass: Class<T>, walker: KFunction1<Node, Sequence<Node>> = Node::walk): List<T> {
    return walker.invoke(this).filterIsInstance(klass).toList()
}

/**
 * Recursively execute "operation" on this node, and all nodes below this node.
 * Every node is informed about its parent node. (But not about the parent's parent!)
 */
fun Node.processConsideringParent(operation: (Node, Node?) -> Unit, parent: Node? = null) {
    operation(this, parent)
    this.containmentProperties.forEach { p ->
        val v = p.get(this)
        when (v) {
            is Node -> v.processConsideringParent(operation, this)
            is Collection<*> -> v.forEach { (it as? Node)?.processConsideringParent(operation, this) }
        }
    }
}

/**
 * @return all direct children of this node.
 */
val Node.children: List<Node>
    get() {
        val children = mutableListOf<Node>()
        containmentProperties.forEach { p ->
            val v = p.get(this)
            when (v) {
                is Node -> children.add(v)
                is Collection<*> -> v.forEach { if (it is Node) children.add(it) }
            }
        }
        return children
    }

/**
 * @param inPlace when false, all nodes will be newly instantiated, otherwise all changes will be "set" on the existing nodes.
 * @return the node and its children transformed with "operation".
 */
fun Node.transform(operation: (Node) -> Node, inPlace: Boolean = false): Node {
    val transformedNode = operation(this)
    return transformedNode.transformChildren(operation, inPlace)
}

class ImmutablePropertyException(property: KProperty<*>, node: Node) :
    RuntimeException("Cannot mutate property '${property.name}' of node $node (class: ${node.javaClass.canonicalName})")

fun Node.transformChildren(operation: (Node) -> Node, inPlace: Boolean = false): Node {
    val changes = mutableMapOf<String, Any>()
    relevantMemberProperties().forEach { p ->
        val v = p.get(this)
        when (v) {
            is Node -> {
                val newValue = operation(v)
                if (newValue != v) {
                    if (inPlace) {
                        if (p is KMutableProperty<*>) {
                            p.setter.call(this, newValue)
                        } else {
                            throw ImmutablePropertyException(p, v)
                        }
                    } else {
                        changes[p.name] = newValue
                    }
                }
            }
            is Collection<*> -> {
                if (inPlace) {
                    if (v is List<*>) {
                        for (i in 0 until v.size) {
                            val element = v[i]
                            if (element is Node) {
                                val newValue = operation(element)
                                if (newValue != element) {
                                    if (v is MutableList<*>) {
                                        (v as MutableList<Node>)[i] = newValue
                                    } else {
                                        throw ImmutablePropertyException(p, element)
                                    }
                                }
                            }
                        }
                    } else {
                        TODO()
                    }
                } else {
                    val newValue = v.map { if (it is Node) operation(it) else it }
                    if (newValue != v) {
                        changes[p.name] = newValue
                    }
                }
            }
        }
    }
    var instanceToTransform = this
    if (!changes.isEmpty()) {
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

fun Node.replace(other: Node) {
    if (this.parent == null) {
        throw IllegalStateException("Parent not set")
    }
    this.parent!!.transformChildren(inPlace = true, operation = { if (it == this) other else it })
}
