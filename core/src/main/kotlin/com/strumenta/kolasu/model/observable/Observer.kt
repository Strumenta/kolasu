package com.strumenta.kolasu.model.observable

import com.strumenta.kolasu.model.Node
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

sealed class NodeNotification<N: Node> {
    abstract val node: N
}
data class AttributeChangedNotification<N: Node, V: Any?>(override val node: N,
                                                          val attributeName: String,
                                                          val oldValue: V, val newValue: V) : NodeNotification<N>()

data class ChildAdded<N: Node>(override val node: N,
    val containmentName: String, val child: Node) : NodeNotification<N>()

data class ChildRemoved<N: Node>(override val node: N,
                                        val containmentName: String, val child: Node) : NodeNotification<N>()

data class ReferenceSet<N: Node>(override val node: N,
                                 val referenceName: String,
                                        val referredNode: Node?) : NodeNotification<N>()

data class ReferencedToAdded<N: Node>(override val node: N,
                                               val referenceName: String,
                                               val referringNode: Node) : NodeNotification<N>()

data class ReferencedToRemoved<N: Node>(override val node: N,
                                      val referenceName: String,
                                      val referringNode: Node) : NodeNotification<N>()



open class SimpleNodeObserver<N : Node> : Subscriber<NodeNotification<N>> {
    open fun <V: Any?>onAttributeChange(node: N, attributeName: String, oldValue: V, newValue: V) {}

    open fun onChildAdded(node: N, containmentName: String, added: Node) {}
    open fun onChildRemoved(node: N, containmentName: String, removed: Node) {}

    open fun onReferenceSet(node: N, referenceName: String, referredNode: Node?) {}

    open fun onReferringAdded(node: N, referenceName: String, referring: Node) {}
    open fun onReferringRemoved(node: N, referenceName: String, referring: Node) {}
    override fun onSubscribe(s: Subscription?) {

    }

    override fun onError(t: Throwable?) {

    }

    override fun onComplete() {

    }

    override fun onNext(notification: NodeNotification<N>) {
        when (notification) {
            is AttributeChangedNotification<N, *> -> onAttributeChange(notification.node, notification.attributeName, notification.oldValue, notification.newValue)
            is ChildAdded<N> -> onChildAdded(notification.node, notification.containmentName, notification.child)
            is ChildRemoved<N> -> onChildRemoved(notification.node, notification.containmentName, notification.child)
            is ReferenceSet<N> -> onReferenceSet(notification.node, notification.referenceName, notification.referredNode)
            is ReferencedToAdded<N> -> onReferenceSet(notification.node, notification.referenceName, notification.referringNode)
            is ReferencedToRemoved<N> -> onReferenceSet(notification.node, notification.referenceName, notification.referringNode)
        }
    }
}