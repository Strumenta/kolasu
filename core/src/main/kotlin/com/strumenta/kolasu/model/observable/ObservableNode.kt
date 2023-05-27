package com.strumenta.kolasu.model.observable

import com.strumenta.kolasu.model.Node

interface Observer<N: Node> {
    fun receivePropertyChangeNotification(node: N, propertyName: String, oldValue: Any?, newValue: Any?)
    fun receivePropertyAddedNotification(node: N, propertyName: String, added: Any?)
    fun receivePropertyRemovedNotification(node: N, propertyName: String, removed: Any?)
}

class MultiplePropertyListObserver<E: Node>(val container: ObservableNode, val propertyName: String) : ListObserver<E> {
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

abstract class ObservableNode : Node() {
    val observers : MutableList<Observer<in ObservableNode>> = mutableListOf()
    fun registerObserver(observer: Observer<*>) {
        observers.add(observer as Observer<in ObservableNode>)
    }

    fun unregisterObserver(observer: Observer<in ObservableNode>) {
        observers.remove(observer)
    }

    protected fun notifyOfPropertyChange(propertyName: String, oldValue: Any?, newValue: Any?) {
        observers.forEach {
            it.receivePropertyChangeNotification(this, propertyName, oldValue, newValue)
        }
    }
}