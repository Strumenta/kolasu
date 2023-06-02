package com.strumenta.kolasu.model.observable

import com.strumenta.kolasu.model.Node
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
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



open class SimpleNodeObserver : Observer<NodeNotification<in Node>> {
    open fun <V: Any?>onAttributeChange(node: Node, attributeName: String, oldValue: V, newValue: V) {}

    open fun onChildAdded(node: Node, containmentName: String, added: Node) {}
    open fun onChildRemoved(node: Node, containmentName: String, removed: Node) {}

    open fun onReferenceSet(node: Node, referenceName: String, referredNode: Node?) {}

    open fun onReferringAdded(node: Node, referenceName: String, referring: Node) {}
    open fun onReferringRemoved(node: Node, referenceName: String, referring: Node) {}

    override fun onSubscribe(d: Disposable) {

    }

    override fun onError(e: Throwable) {

    }

    override fun onComplete() {

    }

    override fun onNext(notification: NodeNotification<in Node>) {
        when (notification) {
            is AttributeChangedNotification<Node, *> -> onAttributeChange(notification.node, notification.attributeName, notification.oldValue, notification.newValue)
            is ChildAdded<Node> -> onChildAdded(notification.node, notification.containmentName, notification.child)
            is ChildRemoved<Node> -> onChildRemoved(notification.node, notification.containmentName, notification.child)
            is ReferenceSet<Node> -> onReferenceSet(notification.node, notification.referenceName, notification.referredNode)
            is ReferencedToAdded<Node> -> onReferenceSet(notification.node, notification.referenceName, notification.referringNode)
            is ReferencedToRemoved<Node> -> onReferenceSet(notification.node, notification.referenceName, notification.referringNode)
        }
    }
}