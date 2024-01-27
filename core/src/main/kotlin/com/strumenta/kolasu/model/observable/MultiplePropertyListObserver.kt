package com.strumenta.kolasu.model.observable

import com.badoo.reaktive.disposable.Disposable
import com.badoo.reaktive.observable.ObservableObserver
import com.strumenta.kolasu.ast.ChildAdded
import com.strumenta.kolasu.ast.ChildRemoved
import com.strumenta.kolasu.ast.NodeLike

class MultiplePropertyListObserver<C : NodeLike, E : NodeLike>(
    val container: C,
    val containmentName: String,
) : ObservableObserver<ListNotification<E>> {
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
