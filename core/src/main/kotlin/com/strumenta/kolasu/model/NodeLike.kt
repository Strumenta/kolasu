package com.strumenta.kolasu.model

import com.badoo.reaktive.observable.ObservableObserver
import com.badoo.reaktive.subject.publish.PublishSubject
import com.strumenta.kolasu.ast.Destination
import com.strumenta.kolasu.ast.Internal
import com.strumenta.kolasu.language.Attribute
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.Reference
import com.strumenta.kolasu.model.annotations.Annotation
import com.strumenta.kolasu.model.observable.NodeNotification
import kotlin.reflect.KClass

typealias NodeObserver = ObservableObserver<in NodeNotification<in NodeLike>>

interface NodeLike {
    @property:Internal
    val changes: PublishSubject<NodeNotification<in NodeLike>>

    @Internal
    val destinations: MutableList<Destination>

    @Internal
    val nodeType: String

    /**
     * The properties of this AST nodes, including attributes, children, and references.
     */
    @property:Internal
    val properties: List<PropertyDescription>

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
    val source: Source?

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
    val allAnnotations: List<Annotation>

    fun <I : Annotation> annotationsByType(kClass: KClass<I>): List<I> {
        return allAnnotations.filterIsInstance(kClass.java)
    }

    fun <I : Annotation> getSingleAnnotation(kClass: KClass<I>): I? {
        val instances = allAnnotations.filterIsInstance(kClass.java)
        return if (instances.isEmpty()) {
            null
        } else if (instances.size == 1) {
            instances.first()
        } else {
            throw IllegalStateException("More than one instance of $kClass found")
        }
    }

    fun <A : Annotation> addAnnotation(annotation: A): A

    fun removeAnnotation(annotation: Annotation)

    fun hasAnnotation(annotation: Annotation): Boolean = allAnnotations.contains(annotation)

    fun getChildren(containment: Containment): List<NodeLike>

    fun getReference(reference: Reference): ReferenceByName<*>

    fun getAttributeValue(attribute: Attribute): Any?

    fun <T : Any?> setAttribute(
        attributeName: String,
        value: T,
    )

    fun <T : Any?> getAttribute(attributeName: String): T

    fun <T : NodeLike> getContainment(containmentName: String): List<T>

    fun <T : NodeLike> addToContainment(
        containmentName: String,
        child: T,
    )

    fun <T : NodeLike> removeFromContainment(
        containmentName: String,
        child: T,
    )

    fun <T : PossiblyNamed> getReference(referenceName: String): ReferenceByName<T>

    fun <T : PossiblyNamed> setReferenceReferred(
        referenceName: String,
        referred: T,
    )

    fun subscribe(observer: ObservableObserver<NodeNotification<in NodeLike>>)
}
