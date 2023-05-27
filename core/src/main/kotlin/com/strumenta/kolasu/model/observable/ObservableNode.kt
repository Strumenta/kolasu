package com.strumenta.kolasu.model.observable

import com.strumenta.kolasu.model.Node

interface Observer<N: Node> {
    fun receivePropertyChangeNotification(node: N, propertyName: String, oldValue: Any?, newValue: Any?)
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