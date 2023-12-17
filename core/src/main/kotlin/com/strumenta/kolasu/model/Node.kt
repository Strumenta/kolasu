package com.strumenta.kolasu.model

import com.strumenta.kolasu.language.Attribute
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.Reference
import com.strumenta.kolasu.model.annotations.Annotation
import com.strumenta.kolasu.model.observable.AttributeChangedNotification
import com.strumenta.kolasu.model.observable.NodeNotification
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.subjects.PublishSubject
import java.io.Serializable
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty1

/**
 * The Abstract Syntax Tree will be constituted by instances of Node.
 */
open class Node() : INode, Serializable {

    @property:Internal
    override val changes = PublishSubject.create<NodeNotification<in INode>>()

    @Internal
    private val annotations: MutableList<Annotation> = mutableListOf()

    @Internal
    override val destinations = mutableListOf<Destination>()

    constructor(range: Range?) : this() {
        this.range = range
    }

    constructor(origin: Origin?) : this() {
        this.origin = origin
    }

    @Internal
    override val nodeType: String
        get() = this::class.qualifiedName!!

    /**
     * The properties of this AST nodes, including attributes, children, and references.
     */
    @property:Internal
    override val properties: List<PropertyDescription>
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
    override var origin: Origin? = null

    /**
     * The parent node, if any.
     */
    @property:Internal
    override var parent: INode? = null

    /**
     * The range of this node in the source text.
     * If a range has been provided when creating this node, it is returned.
     * Otherwise, the value of this property is the range of the origin, if any.
     */
    @property:Internal
    override var range: Range?
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
    override val source: Source?
        get() = origin?.source

    /**
     * The source text for this node
     */
    @Internal
    override val sourceText: String?
        get() = origin?.sourceText

    /**
     * This must be final because otherwise data classes extending this will automatically generate
     * their own implementation. If Link properties are present it could lead to stack overflows in case
     * of circular graphs.
     */
    final override fun toString(): String {
        return "${this.nodeType}(${properties.joinToString(", ") { "${it.name}=${it.valueToString()}" }})"
    }

    protected fun notifyOfPropertyChange(propertyName: String, oldValue: Any?, newValue: Any?) {
        changes.onNext(AttributeChangedNotification(this, propertyName, oldValue, newValue))
    }

    @Internal
    override val allAnnotations: List<Annotation>
        get() = annotations

    override fun <A : Annotation>addAnnotation(annotation: A): A {
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

    override fun removeAnnotation(annotation: Annotation) {
        require(annotation.annotatedNode == this)
        annotations.remove(annotation)
        annotation.detach()
    }

    override fun getChildren(containment: Containment): List<INode> {
        return when (val rawValue = nodeProperties.find { it.name == containment.name }!!.get(this)) {
            null -> {
                emptyList()
            }

            is List<*> -> {
                rawValue as List<INode>
            }

            else -> {
                listOf(rawValue as INode)
            }
        }
    }

    override fun getReference(reference: Reference): ReferenceByName<*> {
        val rawValue = nodeProperties.find { it.name == reference.name }!!.get(this)
        return rawValue as ReferenceByName<*>
    }

    override fun getAttributeValue(attribute: Attribute): Any? {
        return nodeProperties.find { it.name == attribute.name }!!.get(this)
    }

    override fun <T : Any?>setAttribute(attributeName: String, value: T) {
        val prop = nodeProperties.find { it.name == attributeName } as KMutableProperty<T>
        prop.setter.call(this, value)
    }
    override fun <T : Any?>getAttribute(attributeName: String): T {
        val prop = nodeProperties.find { it.name == attributeName }!!
        return prop.call(this) as T
    }

    override fun <T : INode>getContainment(containmentName: String): List<T> {
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

    override fun <T : INode>addToContainment(containmentName: String, child: T) {
        val prop = nodeProperties.find { it.name == containmentName }!!
        val value = prop.call(this)
        if (value is List<*>) {
            (value as MutableList<T>).add(child)
        } else {
            (prop as KMutableProperty<T>).setter.call(this, child)
        }
    }
    override fun <T : INode>removeFromContainment(containmentName: String, child: T) {
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

    override fun <T : PossiblyNamed>getReference(referenceName: String): ReferenceByName<T> {
        val prop = nodeProperties.find { it.name == referenceName } as KProperty1<INode, ReferenceByName<T>>
        return prop.call(this)
    }
    override fun <T : PossiblyNamed>setReferenceReferred(referenceName: String, referred: T) {
        val ref: ReferenceByName<T> = getReference(referenceName)
        ref.referred = referred
    }

    override fun subscribe(observer: Observer<NodeNotification<in INode>>) {
        this.changes.subscribe(observer)
    }
}