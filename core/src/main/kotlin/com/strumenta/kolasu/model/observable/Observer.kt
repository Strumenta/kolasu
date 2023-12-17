package com.strumenta.kolasu.model.observable

import com.strumenta.kolasu.model.INode
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable

sealed class NodeNotification<N : INode> {
    abstract val node: N
}
data class AttributeChangedNotification<N : INode, V : Any?>(
    override val node: N,
    val attributeName: String,
    val oldValue: V,
    val newValue: V
) : NodeNotification<N>()

data class ChildAdded<N : INode>(
    override val node: N,
    val containmentName: String,
    val child: INode
) : NodeNotification<N>()

data class ChildRemoved<N : INode>(
    override val node: N,
    val containmentName: String,
    val child: INode
) : NodeNotification<N>()

data class ReferenceSet<N : INode>(
    override val node: N,
    val referenceName: String,
    val oldReferredNode: INode?,
    val newReferredNode: INode?
) : NodeNotification<N>()

data class ReferencedToAdded<N : INode>(
    override val node: N,
    val referenceName: String,
    val referringNode: INode
) : NodeNotification<N>()

data class ReferencedToRemoved<N : INode>(
    override val node: N,
    val referenceName: String,
    val referringNode: INode
) : NodeNotification<N>()

open class SimpleNodeObserver : Observer<NodeNotification<in INode>> {
    open fun <V : Any?>onAttributeChange(node: INode, attributeName: String, oldValue: V, newValue: V) {}

    open fun onChildAdded(node: INode, containmentName: String, added: INode) {}
    open fun onChildRemoved(node: INode, containmentName: String, removed: INode) {}

    open fun onReferenceSet(node: INode, referenceName: String, oldReferredNode: INode?, newReferredNode: INode?) {}

    open fun onReferringAdded(node: INode, referenceName: String, referring: INode) {}
    open fun onReferringRemoved(node: INode, referenceName: String, referring: INode) {}

    override fun onSubscribe(d: Disposable) {
    }

    override fun onError(e: Throwable) {
    }

    override fun onComplete() {
    }

    override fun onNext(notification: NodeNotification<in INode>) {
        when (notification) {
            is AttributeChangedNotification<INode, *> -> onAttributeChange(
                notification.node,
                notification.attributeName,
                notification.oldValue,
                notification.newValue
            )
            is ChildAdded<INode> -> onChildAdded(notification.node, notification.containmentName, notification.child)
            is ChildRemoved<INode> -> onChildRemoved(
                notification.node,
                notification.containmentName,
                notification.child
            )
            is ReferenceSet<INode> -> onReferenceSet(
                notification.node,
                notification.referenceName,
                notification.oldReferredNode,
                notification.newReferredNode
            )
            is ReferencedToAdded<INode> -> onReferringAdded(
                notification.node,
                notification.referenceName,
                notification.referringNode
            )
            is ReferencedToRemoved<INode> -> onReferringRemoved(
                notification.node,
                notification.referenceName,
                notification.referringNode
            )
        }
    }
}
