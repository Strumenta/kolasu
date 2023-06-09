package com.strumenta.kolasu.model.observable

import com.strumenta.kolasu.model.Node
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable

class MultiplePropertyListObserver<C : Node, E : Node>(
    val container: C,
    val containmentName: String
) : Observer<ListNotification<E>> {
    private fun added(e: E) {
        e.parent = container
        container.changes.onNext(ChildAdded(container, containmentName, e))
    }

    private fun removed(e: E) {
        e.parent = null
        container.changes.onNext(ChildRemoved(container, containmentName, e))
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
