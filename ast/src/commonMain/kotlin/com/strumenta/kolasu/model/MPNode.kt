package com.strumenta.kolasu.model

import com.badoo.reaktive.observable.ObservableObserver
import com.badoo.reaktive.subject.publish.PublishSubject
import com.strumenta.kolasu.exceptions.IllegalStateException
import com.strumenta.kolasu.language.Attribute
import com.strumenta.kolasu.language.Concept
import com.strumenta.kolasu.language.Containment

/**
 * This represents a Multi-platform Kotlin Code. It should be used with the kolasu-multiplatform
 * gradle plugin.
 *
 * Eventually, it should be the only type of node used.
 */
abstract class MPNode : NodeLike {
    @property:Internal
    override val concept: Concept by lazy {
        calculateConcept()
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
    override var parent: NodeLike? = null

    /**
     * This is overriden by the compiler plugin
     */
    protected open fun calculateConcept(): Concept {
        TODO("calculateConcept should be overridden by compiler plugin for $this")
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

    private var explicitlySetSource: Source? = null

    @property:Internal
    override var source: Source?
        get() = explicitlySetSource ?: origin?.source
        set(value) {
            // This is a limit of the current API: to specify a Source we need to specify coordinates
            if (this.range == null) {
                explicitlySetSource = value
            } else {
                this.origin = SimpleOrigin(this.range!!.copy(source = value))
            }
            require(this.source === value)
        }

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
        if (annotation.isSingle) {
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

    override fun getChildren(containment: Containment): List<NodeLike> {
        return when (val rawValue = containment.value(this)) {
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

    override fun <T : NodeLike> getContainmentValue(containmentName: String): List<T> {
        val containment = concept.requireContainment(containmentName)
        return getChildren(containment) as List<T>
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
        TODO("Not yet implemented")
    }

    override fun <T : PossiblyNamed> setReferenceReferred(
        referenceName: String,
        referred: T,
    ) {
        val ref: ReferenceValue<T> = getReference(referenceName)
        ref.referred = referred
    }

    override fun subscribe(observer: ObservableObserver<NodeNotification<in NodeLike>>) {
        this.changes.subscribe(observer)
    }

    fun notifyOfAttributeChange(
        attribute: Attribute,
        oldValue: Any?,
        newValue: Any?,
    ) {
        changes.onNext(AttributeChangedNotification(this, attribute, oldValue, newValue))
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
