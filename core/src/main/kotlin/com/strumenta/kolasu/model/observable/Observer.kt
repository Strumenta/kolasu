package com.strumenta.kolasu.model.observable

import com.strumenta.kolasu.model.Node

interface Observer<N : Node> {
    fun receivePropertyChangeNotification(node: N, propertyName: String, oldValue: Any?, newValue: Any?) {}
    fun receivePropertyAddedNotification(node: N, propertyName: String, added: Any?) {}
    fun receivePropertyRemovedNotification(node: N, propertyName: String, removed: Any?) {}
}
