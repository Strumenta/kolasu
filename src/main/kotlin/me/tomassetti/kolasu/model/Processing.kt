package me.tomassetti.kolasu.model

import java.util.*
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.primaryConstructor

fun Node.process(operation: (Node) -> Unit) {
    operation(this)
    this.javaClass.kotlin.memberProperties
            .filter { it.findAnnotation<Derived>() == null }
            .forEach { p ->
        val v = p.get(this)
        when (v) {
            is Node -> v.process(operation)
            is Collection<*> -> v.forEach { (it as? Node)?.process(operation) }
        }
    }
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
    this.javaClass.kotlin.memberProperties.forEach { p ->
        val v = p.get(this)
        when (v) {
            is Node -> v.processConsideringParent(operation, this)
            is Collection<*> -> v.forEach { (it as? Node)?.processConsideringParent(operation, this) }
        }
    }
}

fun Node.children() : List<Node> {
    val children = LinkedList<Node>()
    this.javaClass.kotlin.memberProperties.filter { it.findAnnotation<Derived>() == null }.forEach { p ->
        val v = p.get(this)
        when (v) {
            is Node -> children.add(v)
            is Collection<*> -> v.forEach { if (it is Node) children.add(it) }
        }
    }
    return children.toList()
}

fun Node.transform(operation: (Node) -> Node) : Node {
    operation(this)
    val changes = HashMap<String, Any>()
    this.javaClass.kotlin.memberProperties.forEach { p ->
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
