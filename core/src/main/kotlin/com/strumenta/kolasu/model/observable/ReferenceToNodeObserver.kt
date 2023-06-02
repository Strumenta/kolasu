package com.strumenta.kolasu.model.observable

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceChangeNotification
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable

class ReferenceToNodeObserver<N: PossiblyNamed>(val container: Node, val referenceName: String) : Observer<ReferenceChangeNotification<N>> {
    override fun onSubscribe(d: Disposable) {

    }

    override fun onError(e: Throwable) {
        TODO("Not yet implemented")
    }

    override fun onComplete() {
        TODO("Not yet implemented")
    }

    override fun onNext(referenceNotification: ReferenceChangeNotification<N>) {
        container.observers.forEach { nodeObserver ->
            nodeObserver.onNext(ReferenceSet(container, referenceName, referenceNotification.oldValue as Node?,
                referenceNotification.newValue as Node?))
            if (referenceNotification.oldValue != null) {
                referenceNotification.oldValue.observers.forEach {
                    it.onNext(ReferencedToRemoved(referenceNotification.oldValue, referenceName, container))
                }
            }
            if (referenceNotification.newValue != null) {
                referenceNotification.newValue.observers.forEach {
                    it.onNext(ReferencedToAdded(referenceNotification.newValue, referenceName, container))
                }
            }
        }
    }
}