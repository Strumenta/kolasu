package com.strumenta.kolasu.model

import com.badoo.reaktive.observable.ObservableObserver
import com.badoo.reaktive.subject.publish.PublishSubject
import com.strumenta.kolasu.language.Annotation
import com.strumenta.kolasu.language.Concept
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.Property
import com.strumenta.kolasu.language.Reference

typealias NodeObserver = ObservableObserver<in NodeNotification<in NodeLike>>

interface NodeLike {
    @property:Internal
    val concept: Concept

    @property:Internal
    val changes: PublishSubject<NodeNotification<in NodeLike>>

    @Internal
    val destinations: MutableList<Destination>

    @Deprecated("Use concept.name")
    @Internal
    val nodeType: String
        get() = concept.name

    @Deprecated("Use concept.qualifiedName")
    @Internal
    val qualifiedNodeType: String
        get() = concept.qualifiedName

    @property:Internal
    val containments: List<Containment>
        get() = concept.declaredFeatures.filterIsInstance<Containment>()

    @property:Internal
    val allContainments: List<Containment>
        get() = concept.allContainments

    /**
     * The origin from which this AST Node has been generated, if any.
     */
    @property:Internal
    var origin: Origin?

    /**
     * The parent node, if any.
     */
    @property:Internal
    var parent: NodeLike?

    /**
     * The range of this node in the source text.
     * If a range has been provided when creating this node, it is returned.
     * Otherwise, the value of this property is the range of the origin, if any.
     */
    @property:Internal
    var range: Range?

    @property:Internal
    var source: Source?

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

    @Internal
    val allAnnotationInstances: List<AnnotationInstance>

    fun annotationsByType(annotation: Annotation): List<AnnotationInstance> {
        return allAnnotationInstances.filter { it.annotation == annotation }
    }

    fun getSingleAnnotation(annotation: Annotation): AnnotationInstance? {
        val instances = annotationsByType(annotation)
        return if (instances.isEmpty()) {
            null
        } else if (instances.size == 1) {
            instances.first()
        } else {
            throw IllegalStateException("More than one instance of $annotation found")
        }
    }

    fun <A : AnnotationInstance> addAnnotation(annotation: A): A

    fun removeAnnotation(annotationInstance: AnnotationInstance)

    fun hasAnnotation(annotationInstance: AnnotationInstance): Boolean =
        allAnnotationInstances.contains(
            annotationInstance,
        )

    fun getChildren(containment: Containment): List<NodeLike> {
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

    fun getPropertyValue(property: Property): Any? {
        return property.value(this)
    }

    fun <T : Any?> setProperty(
        propertyName: String,
        value: T,
    )

    fun <T : Any?> getProperty(propertyName: String): T {
        return concept.requireProperty(propertyName).value(this) as T
    }

    fun <T : NodeLike> getContainmentValue(containmentName: String): List<T>

    fun <T : NodeLike> addToContainment(
        containmentName: String,
        child: T,
    )

    fun <T : NodeLike> removeFromContainment(
        containmentName: String,
        child: T,
    )

    fun getReference(reference: Reference): ReferenceValue<*> {
        val rawValue = reference.value(this)
        return rawValue as ReferenceValue<*>
    }

    fun <T : PossiblyNamed> getReference(name: String): ReferenceValue<T> {
        val rawValue = concept.requireReference(name).value(this)
        return rawValue as ReferenceValue<T>
    }

    fun <T : PossiblyNamed> setReferenceReferred(
        referenceName: String,
        referred: T,
    )

    fun subscribe(observer: ObservableObserver<NodeNotification<in NodeLike>>)
}
