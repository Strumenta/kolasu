package com.strumenta.kolasu.model

import com.badoo.reaktive.observable.ObservableObserver
import com.badoo.reaktive.subject.publish.PublishSubject
import com.strumenta.kolasu.exceptions.IllegalStateException
import com.strumenta.kolasu.language.Attribute
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.Reference

abstract class BaseNode : NodeLike {
    @Internal
    override val nodeType: String = calculateNodeType()

    /**
     * The origin from which this AST Node has been generated, if any.
     */
    @property:Internal
    override var origin: Origin? = null

    /**
     * The parent node, if any.
     */
    @property:Internal
    override var parent: NodeLike? = null

    /**
     * This is overriden by the compiler plugin
     */
    protected open fun calculateFeatures(): List<FeatureDescription> {
        TODO("calculateFeatures should be overridden by compiler plugin")
    }

    /**
     * This is overriden by the compiler plugin
     */
    protected open fun calculateNodeType(): String {
        // We do not want this to crash when initializing subclasses
        return "<UNSPECIFIED>"
    }

    /**
     * The properties of this AST nodes, including attributes, children, and references.
     */
    @property:Internal
    override val features: List<FeatureDescription> by lazy {
        calculateFeatures()
    }

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

    override fun <A : Annotation> addAnnotation(annotation: A): A {
        if (annotation.annotatedNode != null) {
            throw IllegalStateException("Annotation already attached")
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

    override fun <T : NodeLike> addToContainment(
        containmentName: String,
        child: T,
    ) {
        // TODO change FeatureDescription to support this
        TODO("Not yet implemented")
    }

    override fun <T : Any?> getAttribute(attributeName: String): T {
        val feature = features.find { it.name == attributeName }!!
        return feature.value as T
    }

    override fun getAttributeValue(attribute: Attribute): Any? {
        return features.find { it.name == attribute.name }!!.value
    }

    fun getAttributeValue(name: String): Any? {
        return features.find { it.name == name }!!.value
    }

    override fun getChildren(
        containment: Containment,
        includeDerived: Boolean,
    ): List<NodeLike> {
        val property =
            (if (includeDerived) features else originalFeatures)
                .find { it.name == containment.name }
        require(property != null) {
            "Property ${containment.name} not found in node of type ${this.nodeType} " +
                "(considering derived properties? $includeDerived)"
        }
        return when (val rawValue = property!!.value) {
            null -> {
                emptyList()
            }

            is List<*> -> {
                rawValue as List<NodeLike>
            }

            else -> {
                listOf(rawValue as NodeLike)
            }
        }
    }

    override fun <T : NodeLike> getContainment(containmentName: String): List<T> {
        val feature = features.find { it.name == containmentName }!!
        return when (val res = feature.value) {
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

    override fun getReference(reference: Reference): ReferenceByName<*> {
        val rawValue = features.find { it.name == reference.name }!!.value
        return rawValue as ReferenceByName<*>
    }

    override fun <T : PossiblyNamed> getReference(name: String): ReferenceByName<T> {
        val rawValue = features.find { it.name == name }!!.value
        return rawValue as ReferenceByName<T>
    }

    override fun <T : NodeLike> removeFromContainment(
        containmentName: String,
        child: T,
    ) {
        // TODO change FeatureDescription to support this
        TODO("Not yet implemented")
    }

    override fun <T> setAttribute(
        attributeName: String,
        value: T,
    ) {
        // TODO change FeatureDescription to support also setters
        TODO("Not yet implemented")
    }

    override fun <T : PossiblyNamed> setReferenceReferred(
        referenceName: String,
        referred: T,
    ) {
        val ref: ReferenceByName<T> = getReference(referenceName)
        ref.referred = referred
    }

    override fun subscribe(observer: ObservableObserver<NodeNotification<in NodeLike>>) {
        this.changes.subscribe(observer)
    }

    fun notifyOfPropertyChange(
        propertyName: String,
        oldValue: Any?,
        newValue: Any?,
    ) {
        changes.onNext(AttributeChangedNotification(this, propertyName, oldValue, newValue))
    }

    @property:Internal
    override val changes = PublishSubject<NodeNotification<in NodeLike>>()

    @Internal
    private val annotations: MutableList<Annotation> = mutableListOf()

    @Internal
    override val allAnnotations: List<Annotation>
        get() = annotations

    @Internal
    override val destinations = mutableListOf<Destination>()
}
