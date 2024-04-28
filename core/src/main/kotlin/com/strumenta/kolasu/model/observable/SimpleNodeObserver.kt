package com.strumenta.kolasu.model.observable

import com.badoo.reaktive.disposable.Disposable
import com.badoo.reaktive.observable.ObservableObserver
import com.strumenta.kolasu.language.Attribute
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.Reference
import com.strumenta.kolasu.model.AttributeChangedNotification
import com.strumenta.kolasu.model.ChildAdded
import com.strumenta.kolasu.model.ChildRemoved
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.NodeNotification
import com.strumenta.kolasu.model.ReferenceSet
import com.strumenta.kolasu.model.ReferencedToAdded
import com.strumenta.kolasu.model.ReferencedToRemoved

open class SimpleNodeObserver : ObservableObserver<NodeNotification<in NodeLike>> {
    open fun <V : Any?> onAttributeChange(
        node: NodeLike,
        attribute: Attribute,
        oldValue: V,
        newValue: V,
    ) {
    }

    open fun onChildAdded(
        node: NodeLike,
        containment: Containment,
        added: NodeLike,
    ) {
    }

    open fun onChildRemoved(
        node: NodeLike,
        containment: Containment,
        removed: NodeLike,
    ) {
    }

    open fun onReferenceSet(
        node: NodeLike,
        reference: Reference,
        oldReferredNode: NodeLike?,
        newReferredNode: NodeLike?,
    ) {
    }

    open fun onReferringAdded(
        node: NodeLike,
        reference: Reference,
        referring: NodeLike,
    ) {
    }

    open fun onReferringRemoved(
        node: NodeLike,
        reference: Reference,
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
                    notification.attribute,
                    notification.oldValue,
                    notification.newValue,
                )
            is ChildAdded<NodeLike> -> onChildAdded(notification.node, notification.containment, notification.child)
            is ChildRemoved<NodeLike> ->
                onChildRemoved(
                    notification.node,
                    notification.containment,
                    notification.child,
                )

            is ReferenceSet<NodeLike> ->
                onReferenceSet(
                    notification.node,
                    notification.reference,
                    notification.oldReferredNode,
                    notification.newReferredNode,
                )

            is ReferencedToAdded<NodeLike> ->
                onReferringAdded(
                    notification.node,
                    notification.reference,
                    notification.referringNode,
                )
            is ReferencedToRemoved<NodeLike> ->
                onReferringRemoved(
                    notification.node,
                    notification.reference,
                    notification.referringNode,
                )
        }
    }
}
