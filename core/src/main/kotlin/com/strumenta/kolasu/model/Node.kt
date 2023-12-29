package com.strumenta.kolasu.model

import com.badoo.reaktive.observable.ObservableObserver
import com.badoo.reaktive.subject.publish.PublishSubject
import com.strumenta.kolasu.language.Attribute
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.Reference
import com.strumenta.kolasu.model.annotations.Annotation
import com.strumenta.kolasu.model.observable.AttributeChangedNotification
import com.strumenta.kolasu.model.observable.NodeNotification
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty1

typealias NodeObserver = ObservableObserver<in NodeNotification<in Node>>

/**
 * The Abstract Syntax Tree will be constituted by instances of Node.
 */
open class Node : Serializable {
    @property:Internal
    val changes = PublishSubject<NodeNotification<in Node>>()

    @Internal
    private val annotations: MutableList<Annotation> = mutableListOf()

    @Internal
    val destinations = mutableListOf<Destination>()

    constructor()

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
        get() =
            try {
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

    protected fun notifyOfPropertyChange(
        propertyName: String,
        oldValue: Any?,
        newValue: Any?,
    ) {
        changes.onNext(AttributeChangedNotification(this, propertyName, oldValue, newValue))
    }

    @Internal
    val allAnnotations: List<Annotation>
        get() = annotations

    fun <I : Annotation> annotationsByType(kClass: KClass<I>): List<I> {
        return annotations.filterIsInstance(kClass.java)
    }

    fun <I : Annotation> getSingleAnnotation(kClass: KClass<I>): I? {
        val instances = annotations.filterIsInstance(kClass.java)
        return if (instances.isEmpty()) {
            null
        } else if (instances.size == 1) {
            instances.first()
        } else {
            throw IllegalStateException("More than one instance of $kClass found")
        }
    }

    fun <A : Annotation> addAnnotation(annotation: A): A {
        if (annotation.annotatedNode != null) {
            throw java.lang.IllegalStateException("Annotation already attached")
        }
        annotation.attachTo(this)
        if (annotation.single) {
            annotations.filter { it.annotationType == annotation.annotationType }.forEach { removeAnnotation(it) }
        }
        annotations.add(annotation)
        return annotation
    }

    fun removeAnnotation(annotation: Annotation) {
        require(annotation.annotatedNode == this)
        annotations.remove(annotation)
        annotation.detach()
    }

    fun hasAnnotation(annotation: Annotation): Boolean {
        return annotations.contains(annotation)
    }

    fun getChildren(containment: Containment): List<Node> {
        return when (val rawValue = nodeProperties.find { it.name == containment.name }!!.get(this)) {
            null -> {
                emptyList()
            }

            is List<*> -> {
                rawValue as List<Node>
            }

            else -> {
                listOf(rawValue as Node)
            }
        }
    }

    fun getReference(reference: Reference): ReferenceByName<*> {
        val rawValue = nodeProperties.find { it.name == reference.name }!!.get(this)
        return rawValue as ReferenceByName<*>
    }

    fun getAttributeValue(attribute: Attribute): Any? {
        return nodeProperties.find { it.name == attribute.name }!!.get(this)
    }

    fun <T : Any?> setAttribute(
        attributeName: String,
        value: T,
    ) {
        val prop = nodeProperties.find { it.name == attributeName } as KMutableProperty<T>
        prop.setter.call(this, value)
    }

    fun <T : Any?> getAttribute(attributeName: String): T {
        val prop = nodeProperties.find { it.name == attributeName }!!
        return prop.call(this) as T
    }

    fun <T : Node> getContainment(containmentName: String): List<T> {
        val prop = nodeProperties.find { it.name == containmentName }!!
        return when (val res = prop.call(this)) {
            null -> {
                emptyList()
            }

            is List<*> -> {
                res as List<T>
            }

            else -> {
                listOf(res as T)
            }
        }
    }

    fun <T : Node> addToContainment(
        containmentName: String,
        child: T,
    ) {
        val prop = nodeProperties.find { it.name == containmentName }!!
        val value = prop.call(this)
        if (value is List<*>) {
            (value as MutableList<T>).add(child)
        } else {
            (prop as KMutableProperty<T>).setter.call(this, child)
        }
    }

    fun <T : Node> removeFromContainment(
        containmentName: String,
        child: T,
    ) {
        val prop = nodeProperties.find { it.name == containmentName }!!
        val value = prop.call(this)
        if (value is List<*>) {
            (value as MutableList<T>).remove(child)
        } else {
            if (value == child) {
                (prop as KMutableProperty<T>).setter.call(this, null)
            }
        }
    }

    fun <T : PossiblyNamed> getReference(referenceName: String): ReferenceByName<T> {
        val prop = nodeProperties.find { it.name == referenceName } as KProperty1<Node, ReferenceByName<T>>
        return prop.call(this)
    }

    fun <T : PossiblyNamed> setReferenceReferred(
        referenceName: String,
        referred: T,
    ) {
        val ref: ReferenceByName<T> = getReference(referenceName)
        ref.referred = referred
    }

    fun subscribe(observer: ObservableObserver<NodeNotification<in Node>>) {
        this.changes.subscribe(observer)
    }
}
