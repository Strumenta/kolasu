package com.strumenta.kolasu.model.observable

import com.strumenta.kolasu.model.Node

class MultiplePropertyListObserver<E : Node>(val container: Node, val propertyName: String) : ListObserver<E> {
    override fun added(e: E) {
        e.parent = container
        container.observers.forEach {
            it.receivePropertyAddedNotification(container, propertyName, e)
        }
    }

    override fun removed(e: E) {
        e.parent = null
        container.observers.forEach {
            it.receivePropertyRemovedNotification(container, propertyName, e)
        }
    }
}
