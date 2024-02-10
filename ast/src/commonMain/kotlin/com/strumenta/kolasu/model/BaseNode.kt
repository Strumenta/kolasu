package com.strumenta.kolasu.model

import com.badoo.reaktive.observable.ObservableObserver
import com.badoo.reaktive.subject.publish.PublishSubject
import com.strumenta.kolasu.language.Attribute
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.Reference

abstract class BaseNode : NodeLike {
    override val allAnnotations: List<Annotation>
        get() = TODO("Not yet implemented")
    override val changes: PublishSubject<NodeNotification<in NodeLike>>
        get() = TODO("Not yet implemented")
    override val destinations: MutableList<Destination>
        get() = TODO("Not yet implemented")
    override val nodeType: String
        get() = TODO("Not yet implemented")

    override var origin: Origin? = null

    override var parent: NodeLike? = null
    override val properties: List<FeatureDescription>
        get() = TODO("Not yet implemented")
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

    override fun <T : NodeLike> addToContainment(containmentName: String, child: T) {
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

    override fun <T : NodeLike> removeFromContainment(containmentName: String, child: T) {
        TODO("Not yet implemented")
    }

    override fun <T> setAttribute(attributeName: String, value: T) {
        TODO("Not yet implemented")
    }

    override fun <T : PossiblyNamed> setReferenceReferred(referenceName: String, referred: T) {
        TODO("Not yet implemented")
    }

    override fun subscribe(observer: ObservableObserver<NodeNotification<in NodeLike>>) {
        TODO("Not yet implemented")
    }
}