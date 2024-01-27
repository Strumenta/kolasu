package com.strumenta.kolasu.model.observable

import com.badoo.reaktive.disposable.Disposable
import com.badoo.reaktive.observable.ObservableObserver
import com.strumenta.kolasu.ast.AttributeChangedNotification
import com.strumenta.kolasu.ast.ChildAdded
import com.strumenta.kolasu.ast.ChildRemoved
import com.strumenta.kolasu.ast.NodeLike
import com.strumenta.kolasu.ast.NodeNotification
import com.strumenta.kolasu.ast.ReferenceSet
import com.strumenta.kolasu.ast.ReferencedToAdded
import com.strumenta.kolasu.ast.ReferencedToRemoved

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
