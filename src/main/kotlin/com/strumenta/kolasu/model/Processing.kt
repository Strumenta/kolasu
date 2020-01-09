package com.strumenta.kolasu.model

import java.util.LinkedList
import kotlin.collections.HashMap
import kotlin.reflect.*
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

private val <T : Node> T.containmentProperties: Collection<KProperty1<T, *>>
    get() = this.javaClass.kotlin.memberProperties
            .filter { it.visibility == KVisibility.PUBLIC }
            .filter { it.findAnnotation<Derived>() == null }
            .filter { it.findAnnotation<Link>() == null }
            .filter { it.name != "parent" }

fun Node.assignParents() {
    this.children.forEach {
        it.parent = this
        it.assignParents()
    }
}

fun Node.processNodes(operation: (Node) -> Unit) {
    operation(this)
    containmentProperties.forEach { p ->
        val v = p.get(this)
        when {
            v is Node -> v.processNodes(operation)
            v is Collection<*> -> v.forEach { (it as? Node)?.processNodes(operation) }
        }
    }
}

private fun provideNodes(kTypeProjection: KTypeProjection): Boolean {
    val ktype = kTypeProjection.type
    return when (ktype) {
        is KClass<*> -> provideNodes(ktype as? KClass<*>)
        is KType -> provideNodes((ktype as? KType)?.classifier)
        else -> TODO()
    }
}

private fun provideNodes(classifier: KClassifier?): Boolean {
    if (classifier is KClass<*>) {
        return provideNodes(classifier as? KClass<*>)
    } else {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}

private fun provideNodes(kclass: KClass<*>?): Boolean {
    return kclass?.representsNode() ?: false
}

fun KClass<*>.representsNode(): Boolean {
    return this.isSubclassOf(Node::class) || this.isMarkedAsNodeType()
}

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

fun Node.find(predicate: (Node) -> Boolean): Node? {
    if (predicate(this)) {
        return this
    }
    containmentProperties.forEach { p ->
        when (val v = p.get(this)) {
            is Node -> {
                val res = v.find(predicate)
                if (res != null) {
                    return res
                }
            }
            is Collection<*> -> v.forEach {
                (it as? Node)?.let {
                    val res = it.find(predicate)
                    if (res != null) {
                        return res
                    }
                }
            }
        }
    }
    return null
}

fun <T : Node> Node.specificProcess(klass: Class<T>, operation: (T) -> Unit) {
    processNodes {
        if (klass.isInstance(it)) {
            operation(it as T)
        }
    }
}

fun <T : Node> Node.collectByType(klass: Class<T>): List<T> {
    val res = LinkedList<T>()
    this.specificProcess(klass, { res.add(it) })
    return res
}

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

val Node.children: List<Node>
    get() {
        val children = LinkedList<Node>()
        containmentProperties.forEach { p ->
            val v = p.get(this)
            when (v) {
                is Node -> children.add(v)
                is Collection<*> -> v.forEach { if (it is Node) children.add(it) }
            }
        }
        return children.toList()
    }

// TODO reimplement using transformChildren
fun Node.transform(operation: (Node) -> Node, inPlace: Boolean = false): Node {
    if (inPlace) TODO()
    operation(this)
    val changes = HashMap<String, Any>()
    relevantMemberProperties().forEach { p ->
        val v = p.get(this)
        when (v) {
            is Node -> {
                val newValue = v.transform(operation)
                if (newValue != v) changes[p.name] = newValue
            }
            is Collection<*> -> {
                val newValue = v.map { if (it is Node) it.transform(operation) else it }
                if (newValue != v) changes[p.name] = newValue
            }
        }
    }
    var instanceToTransform = this
    if (!changes.isEmpty()) {
        val constructor = this.javaClass.kotlin.primaryConstructor!!
        val params = HashMap<KParameter, Any?>()
        constructor.parameters.forEach { param ->
            if (changes.containsKey(param.name)) {
                params[param] = changes[param.name]
            } else {
                params[param] = this.javaClass.kotlin.memberProperties.find { param.name == it.name }!!.get(this)
            }
        }
        instanceToTransform = constructor.callBy(params)
    }
    return operation(instanceToTransform)
}

class ImmutablePropertyException(property: KProperty<*>, node: Node) :
        RuntimeException("Cannot mutate property '${property.name}' of node $node (class: ${node.javaClass.canonicalName})")

fun Node.transformChildrenInPlace(operation: (Node) -> Node) {
    relevantMemberProperties().forEach { property ->
        val value = property.get(this)
        when (value) {
            is Node -> {
                val newValue = operation(value)
                if (newValue != value) {
                    if (property is KMutableProperty<*>) {
                        property.setter.call(this, newValue)
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
                                } else {
                                    throw ImmutablePropertyException(property, element)
                                }
                            }
                        }
                    }
                } else {
                    TODO()
                }
            }
        }
    }
}

fun Node.transformChildren(operation: (Node) -> Node): Node {
    val changes = HashMap<String, Any>()
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
    if (!changes.isEmpty()) {
        val constructor = this.javaClass.kotlin.primaryConstructor!!
        val params = HashMap<KParameter, Any?>()
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
    this.parent!!.transformChildrenInPlace { if (it == this) other else it }
}
