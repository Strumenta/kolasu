package com.strumenta.kolasu.lionweb

import com.badoo.reaktive.observable.ObservableObserver
import com.badoo.reaktive.subject.publish.PublishSubject
import com.strumenta.kolasu.language.Concept
import com.strumenta.kolasu.model.AnnotationInstance
import com.strumenta.kolasu.model.Destination
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.NodeNotification
import com.strumenta.kolasu.model.Origin
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.Range
import com.strumenta.kolasu.model.Source

data class ProxyNode(
    val nodeId: String,
) : KNode {
    override val concept: Concept
        get() = TODO("Not yet implemented")
    override val changes: PublishSubject<NodeNotification<in NodeLike>>
        get() = TODO("Not yet implemented")
    override val destinations: MutableList<Destination>
        get() = TODO("Not yet implemented")
    override var origin: Origin?
        get() = TODO("Not yet implemented")
        set(value) {}
    override var parent: NodeLike?
        get() = TODO("Not yet implemented")
        set(value) {}
    override var range: Range?
        get() = TODO("Not yet implemented")
        set(value) {}
    override var source: Source?
        get() = TODO("Not yet implemented")
        set(value) {}
    override val sourceText: String?
        get() = TODO("Not yet implemented")
    override val allAnnotationInstances: List<AnnotationInstance>
        get() = TODO("Not yet implemented")

    override fun <A : AnnotationInstance> addAnnotation(annotation: A): A {
        TODO("Not yet implemented")
    }

    override fun removeAnnotation(annotationInstance: AnnotationInstance) {
        TODO("Not yet implemented")
    }

    override fun <T> setPropertySimpleValue(
        propertyName: String,
        value: T,
    ) {
        TODO("Not yet implemented")
    }

    override fun <T : NodeLike> getContainmentValue(containmentName: String): List<T> {
        TODO("Not yet implemented")
    }

    override fun <T : NodeLike> addToContainment(
        containmentName: String,
        child: T,
    ) {
        TODO("Not yet implemented")
    }

    override fun <T : NodeLike> removeFromContainment(
        containmentName: String,
        child: T,
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
}

object ProxyBasedNodeResolver : NodeResolver {
    override fun resolve(nodeID: String): KNode? = ProxyNode(nodeID)
}
