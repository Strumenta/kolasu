package com.strumenta.kolasu.model

import com.strumenta.kolasu.model.annotations.AnnotationInstance
import com.strumenta.kolasu.model.observable.Observer
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

/**
 * The Abstract Syntax Tree will be constituted by instances of Node.
 */
open class Node() : Serializable {

    private val annotations: MutableList<AnnotationInstance> = mutableListOf()

    fun getAnnotations() : List<AnnotationInstance> {
        return annotations
    }

    fun <I: AnnotationInstance>getAnnotations(kClass: KClass<I>) : List<I> {
        return annotations.filterIsInstance(kClass.java)
    }

    fun <I: AnnotationInstance>getSingleAnnotations(kClass: KClass<I>) : I? {
        val instances = annotations.filterIsInstance(kClass.java)
        return if (instances.isEmpty()) {
            null
        } else if (instances.size == 1) {
            instances.first()
        } else {
            throw IllegalStateException("More than one instance of $kClass found")
        }
    }

    fun addAnnotation(annotationInstance: AnnotationInstance) {
        require(annotationInstance.annotatedNode == this)
        if (annotationInstance.type.single) {
            annotations.removeIf { it.type == annotationInstance.type}
        }
        annotations.add(annotationInstance)
    }

    fun removeAnnotation(annotationInstance: AnnotationInstance) {
        require(annotationInstance.annotatedNode == this)
        annotations.remove(annotationInstance)
    }

    @Internal
    val destinations = mutableListOf<Destination>()

    constructor(range: Range?) : this() {
        this.range = range
    }

    constructor(origin: Origin?) : this() {
        this.origin = origin
    }

    @Internal
    open val nodeType: String
        get() = this::class.qualifiedName!!

    /**
     * The properties of this AST nodes, including attributes, children, and references.
     */
    @property:Internal
    open val properties: List<PropertyDescription>
        get() = try {
            nodeProperties.map { PropertyDescription.buildFor(it, this) }
        } catch (e: Throwable) {
            throw RuntimeException("Issue while getting properties of node ${this::class.qualifiedName}", e)
        }.also { properties ->
            val alreadyFound = mutableSetOf<String>()
            properties.forEach { property ->
                val name = property.name
                if (alreadyFound.contains(name)) {
                    throw IllegalStateException("Duplicate property with name $name")
                } else {
                    alreadyFound.add(name)
                }
            }
        }

    /**
     * The origin from which this AST Node has been generated, if any.
     */
    @property:Internal
    var origin: Origin? = null

    /**
     * The parent node, if any.
     */
    @property:Internal
    var parent: Node? = null

    /**
     * The range of this node in the source text.
     * If a range has been provided when creating this node, it is returned.
     * Otherwise, the value of this property is the range of the origin, if any.
     */
    @property:Internal
    var range: Range?
        get() = origin?.range
        set(value) {
            if (origin == null) {
                if (value != null) {
                    origin = SimpleOrigin(value)
                }
            } else {
                origin!!.range = value
            }
        }

    @property:Internal
    val source: Source?
        get() = origin?.source

    /**
     * Tests whether the given range is contained in the interval represented by this object.
     * @param range the range
     */
    fun contains(range: Range?): Boolean {
        return this.range?.contains(range) ?: false
    }

    /**
     * Tests whether the given range overlaps the interval represented by this object.
     * @param range the range
     */
    fun overlaps(range: Range?): Boolean {
        return this.range?.overlaps(range) ?: false
    }

    /**
     * The source text for this node
     */
    @Internal
    val sourceText: String?
        get() = origin?.sourceText

    /**
     * This must be final because otherwise data classes extending this will automatically generate
     * their own implementation. If Link properties are present it could lead to stack overflows in case
     * of circular graphs.
     */
    final override fun toString(): String {
        return "${this.nodeType}(${properties.joinToString(", ") { "${it.name}=${it.valueToString()}" }})"
    }

    @property:Internal
    val observers: MutableList<Observer<in Node>> = mutableListOf()
    fun registerObserver(observer: Observer<*>) {
        observers.add(observer as Observer<in Node>)
    }

    fun unregisterObserver(observer: Observer<in Node>) {
        observers.remove(observer)
    }

    protected fun notifyOfPropertyChange(propertyName: String, oldValue: Any?, newValue: Any?) {
        observers.forEach {
            it.receivePropertyChangeNotification(this, propertyName, oldValue, newValue)
        }
    }
}

fun <N : Node> N.withRange(range: Range?): N {
    this.range = range
    return this
}

fun <N : Node> N.withOrigin(origin: Origin?): N {
    this.origin = if (origin == NodeOrigin(this)) { null } else { origin }
    return this
}

fun <N : Node> N.withOrigin(node: Node): N {
    this.origin = if (node == this) { null } else { NodeOrigin(node) }
    return this
}

val <T : Any> Class<T>.nodeProperties: Collection<KProperty1<T, *>>
    get() = this.kotlin.nodeProperties
val <T : Any> KClass<T>.nodeProperties: Collection<KProperty1<T, *>>
    get() = memberProperties.asSequence()
        .filter { it.visibility == KVisibility.PUBLIC }
        .filter { it.findAnnotation<Derived>() == null }
        .filter { it.findAnnotation<Internal>() == null }
        .filter { it.findAnnotation<Link>() == null }
        .toList()

/**
 * @return all properties of this node that are considered AST properties.
 */
val <T : Node> T.nodeProperties: Collection<KProperty1<T, *>>
    get() = this.javaClass.nodeProperties

/**
 * Use this to mark properties that are internal, i.e., they are used for bookkeeping and are not part of the model,
 * so that they will not be considered branches of the AST.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Internal

/**
 * Use this to mark all relations which are secondary, i.e., they are calculated from other relations,
 * so that they will not be considered branches of the AST.
 */
annotation class Derived

/**
 * Use this to mark all the properties that return a Node or a list of Nodes which are not
 * contained by the Node having the properties. In other words: they are just references.
 * This will prevent them from being considered branches of the AST.
 */
annotation class Link

/**
 * Use this to mark something that does not inherit from Node as a node, so it will be included in the AST.
 */
annotation class NodeType
