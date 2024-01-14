package com.strumenta.kolasu.model.observable

import com.badoo.reaktive.disposable.Disposable
import com.badoo.reaktive.observable.ObservableObserver
import com.strumenta.kolasu.model.NodeLike

sealed class NodeNotification<N : NodeLike> {
    abstract val node: N
}

data class AttributeChangedNotification<N : NodeLike, V : Any?>(
    override val node: N,
    val attributeName: String,
    val oldValue: V,
    val newValue: V,
) : NodeNotification<N>()

data class ChildAdded<N : NodeLike>(
    override val node: N,
    val containmentName: String,
    val child: NodeLike,
) : NodeNotification<N>()

data class ChildRemoved<N : NodeLike>(
    override val node: N,
    val containmentName: String,
    val child: NodeLike,
) : NodeNotification<N>()

data class ReferenceSet<N : NodeLike>(
    override val node: N,
    val referenceName: String,
    val oldReferredNode: NodeLike?,
    val newReferredNode: NodeLike?,
) : NodeNotification<N>()

data class ReferencedToAdded<N : NodeLike>(
    override val node: N,
    val referenceName: String,
    val referringNode: NodeLike,
) : NodeNotification<N>()

data class ReferencedToRemoved<N : NodeLike>(
    override val node: N,
    val referenceName: String,
    val referringNode: NodeLike,
) : NodeNotification<N>()

open class SimpleNodeObserver : ObservableObserver<NodeNotification<in NodeLike>> {
    open fun <V : Any?> onAttributeChange(
        node: NodeLike,
        attributeName: String,
        oldValue: V,
        newValue: V,
    ) {
    }

    open fun onChildAdded(
        node: NodeLike,
        containmentName: String,
        added: NodeLike,
    ) {
    }

    open fun onChildRemoved(
        node: NodeLike,
        containmentName: String,
        removed: NodeLike,
    ) {
    }

    open fun onReferenceSet(
        node: NodeLike,
        referenceName: String,
        oldReferredNode: NodeLike?,
        newReferredNode: NodeLike?,
    ) {
    }

    open fun onReferringAdded(
        node: NodeLike,
        referenceName: String,
        referring: NodeLike,
    ) {
    }

    open fun onReferringRemoved(
        node: NodeLike,
        referenceName: String,
        referring: NodeLike,
    ) {
    }

    override fun onSubscribe(d: Disposable) {
    }

    override fun onError(e: Throwable) {
    }

    override fun onComplete() {
    }

    override fun onNext(notification: NodeNotification<in NodeLike>) {
        when (notification) {
            is AttributeChangedNotification<NodeLike, *> ->
                onAttributeChange(
                    notification.node,
                    notification.attributeName,
                    notification.oldValue,
                    notification.newValue,
                )
            is ChildAdded<NodeLike> -> onChildAdded(notification.node, notification.containmentName, notification.child)
            is ChildRemoved<NodeLike> ->
                onChildRemoved(
                    notification.node,
                    notification.containmentName,
                    notification.child,
                )

            is ReferenceSet<NodeLike> ->
                onReferenceSet(
                    notification.node,
                    notification.referenceName,
                    notification.oldReferredNode,
                    notification.newReferredNode,
                )

            is ReferencedToAdded<NodeLike> ->
                onReferringAdded(
                    notification.node,
                    notification.referenceName,
                    notification.referringNode,
                )
            is ReferencedToRemoved<NodeLike> ->
                onReferringRemoved(
                    notification.node,
                    notification.referenceName,
                    notification.referringNode,
                )
        }
    }
}
