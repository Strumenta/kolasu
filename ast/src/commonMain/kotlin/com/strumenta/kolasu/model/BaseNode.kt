package com.strumenta.kolasu.model

import com.badoo.reaktive.observable.ObservableObserver
import com.badoo.reaktive.subject.publish.PublishSubject
import com.strumenta.kolasu.language.Attribute
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.Reference

abstract class BaseNode : NodeLike {
    override val nodeType: String = calculateNodeType()

    override var origin: Origin? = null

    override var parent: NodeLike? = null

    protected open fun calculateFeatures() : List<FeatureDescription> {
        TODO("Not yet implemented")
    }

    protected open fun calculateNodeType() : String {
        // We do not want this to crash when initializing subclasses
        return "<UNSPECIFIED>"
    }

    override val properties: List<FeatureDescription> by lazy {
        calculateFeatures()
    }
    override var range: Range?
        get() = TODO("Not yet implemented")
        set(value) {}
    override val source: Source?
        get() = TODO("Not yet implemented")
    override val sourceText: String?
        get() = TODO("Not yet implemented")

    override fun <A : Annotation> addAnnotation(annotation: A): A {
        TODO("Not yet implemented")
    }

    override fun <T : NodeLike> addToContainment(
        containmentName: String,
        child: T,
    ) {
        TODO("Not yet implemented")
    }

    override fun <T> getAttribute(attributeName: String): T {
        TODO("Not yet implemented")
    }

    override fun getAttributeValue(attribute: Attribute): Any? {
        TODO("Not yet implemented")
    }

    override fun getChildren(containment: Containment): List<NodeLike> {
        TODO("Not yet implemented")
    }

    override fun <T : NodeLike> getContainment(containmentName: String): List<T> {
        TODO("Not yet implemented")
    }

    override fun getReference(reference: Reference): ReferenceByName<*> {
        TODO("Not yet implemented")
    }

    override fun <T : PossiblyNamed> getReference(referenceName: String): ReferenceByName<T> {
        TODO("Not yet implemented")
    }

    override fun removeAnnotation(annotation: Annotation) {
        TODO("Not yet implemented")
    }

    override fun <T : NodeLike> removeFromContainment(
        containmentName: String,
        child: T,
    ) {
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
        TODO("Not yet implemented")
    }

    override fun subscribe(observer: ObservableObserver<NodeNotification<in NodeLike>>) {
        TODO("Not yet implemented")
    }

    protected fun notifyOfPropertyChange(
        propertyName: String,
        oldValue: Any?,
        newValue: Any?,
    ) {
        changes.onNext(AttributeChangedNotification(this, propertyName, oldValue, newValue))
    }
    @property:Internal
    override val changes = PublishSubject<NodeNotification<in NodeLike>>()

    @Internal
    private val annotations: MutableList<kotlin.Annotation> = mutableListOf()

    override val allAnnotations: List<Annotation>
        get() = TODO("Not yet implemented")

    @Internal
    override val destinations = mutableListOf<Destination>()


}
