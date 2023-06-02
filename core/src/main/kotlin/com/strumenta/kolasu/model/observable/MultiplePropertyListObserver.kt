package com.strumenta.kolasu.model.observable

import com.strumenta.kolasu.model.Node

class MultiplePropertyListObserver<C: Node, E : Node>(val container: C, val containmentName: String) : ListObserver<E> {
    override fun added(e: E) {
        e.parent = container
        container.observers.forEach {
            it.onNext(ChildAdded(container, containmentName, e))
        }
    }

    override fun removed(e: E) {
        e.parent = null
        container.observers.forEach {
            it.onNext(ChildRemoved(container, containmentName, e))
        }
    }
}
