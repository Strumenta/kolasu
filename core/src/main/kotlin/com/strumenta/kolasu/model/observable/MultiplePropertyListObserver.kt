package com.strumenta.kolasu.model.observable

import com.badoo.reaktive.disposable.Disposable
import com.badoo.reaktive.observable.ObservableObserver
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.model.ChildAdded
import com.strumenta.kolasu.model.ChildRemoved
import com.strumenta.kolasu.model.NodeLike

class MultiplePropertyListObserver<C : NodeLike, E : NodeLike>(
    val container: C,
    val containment: Containment,
) : ObservableObserver<ListNotification<E>> {
    constructor(
        container: C,
        containmentName: String,
    ) : this(container, container.concept.requireContainment(containmentName))

    private fun added(e: E) {
        e.parent = container
        container.changes.onNext(ChildAdded(container, containment, e))
    }

    private fun removed(e: E) {
        e.parent = null
        container.changes.onNext(ChildRemoved(container, containment, e))
    }

    override fun onSubscribe(d: Disposable) {
    }

    override fun onError(e: Throwable) {
    }

    override fun onComplete() {
    }

    override fun onNext(notification: ListNotification<E>) {
        when (notification) {
            is ListAddition -> added(notification.added)
            is ListRemoval -> removed(notification.removed)
        }
    }
}
