package com.strumenta.kolasu.model.observable

import com.strumenta.kolasu.model.Node
import org.reactivestreams.Subscriber

sealed class NodeNotification<N: Node> {
    abstract val node: N
}
data class PropertyChangeNodeNotification<N: Node>(override val node: N) : NodeNotification<N>()

interface Observer<N : Node> : Subscriber<NodeNotification<N>> {
    fun receivePropertyChangeNotification(node: N, propertyName: String, oldValue: Any?, newValue: Any?) {
        onNext(PropertyChangeNodeNotification(node))
    }
    fun receivePropertyAddedNotification(node: N, propertyName: String, added: Any?) {}
    fun receivePropertyRemovedNotification(node: N, propertyName: String, removed: Any?) {}
}
