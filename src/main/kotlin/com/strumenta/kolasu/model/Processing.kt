package com.strumenta.kolasu.model

import java.util.*
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

private val <T: Node> T.relevantProperties : Collection<KProperty1<T, *>>
    get() = this.javaClass.kotlin.memberProperties
            .filter { it.findAnnotation<Derived>() == null }
            .filter { it.name != "parent" }

fun Node.assignParents() {
    this.children.forEach {
        it.parent = this
        it.assignParents()
    }
}

fun Node.process(operation: (Node) -> Unit) {
    operation(this)
    relevantProperties.forEach { p ->
        val v = p.get(this)
        when (v) {
            is Node -> v.process(operation)
            is Collection<*> -> v.forEach { (it as? Node)?.process(operation) }
        }
    }
}

fun Node.find(predicate: (Node) -> Boolean) : Node? {
    if (predicate(this)) {
        return this
    }
    relevantProperties.forEach { p ->
        val v = p.get(this)
        when (v) {
            is Node -> {
                val res = v.find(predicate)
                if (res != null) {
                    return res
                }
            }
            is Collection<*> -> v.forEach { (it as? Node)?.let {
                val res = it.find(predicate)
                if (res != null) {
                    return res
                }
            } }
        }
    }
    return null
}

fun <T: Node> Node.specificProcess(klass: Class<T>, operation: (T) -> Unit) {
    process { if (klass.isInstance(it)) {
        operation(it as T) }
    }
}

fun <T: Node> Node.collectByType(klass: Class<T>) : List<T> {
    val res = LinkedList<T>()
    this.specificProcess(klass, {res.add(it)})
    return res
}

fun Node.processConsideringParent(operation: (Node, Node?) -> Unit, parent: Node? = null) {
    operation(this, parent)
    this.relevantProperties.forEach { p ->
        val v = p.get(this)
        when (v) {
            is Node -> v.processConsideringParent(operation, this)
            is Collection<*> -> v.forEach { (it as? Node)?.processConsideringParent(operation, this) }
        }
    }
}

val Node.children : List<Node>
    get() {
        val children = LinkedList<Node>()
        relevantProperties.forEach { p ->
            val v = p.get(this)
            when (v) {
                is Node -> children.add(v)
                is Collection<*> -> v.forEach { if (it is Node) children.add(it) }
            }
        }
        return children.toList()
    }

// TODO reimplement using transformChildren
fun Node.transform(operation: (Node) -> Node, inPlace: Boolean = false) : Node {
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

class ImmutablePropertyException(property: KProperty<*>) : RuntimeException("Cannot mutate property ${property.name}")

fun Node.transformChildren(operation: (Node) -> Node, inPlace: Boolean = false) : Node {
    val changes = HashMap<String, Any>()
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
                            throw ImmutablePropertyException(p)
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
                                        throw ImmutablePropertyException(p)
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
    this.parent!!.transformChildren(inPlace = true, operation = { if (it == this) other else it } )
}
