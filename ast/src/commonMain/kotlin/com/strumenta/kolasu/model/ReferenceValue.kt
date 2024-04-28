package com.strumenta.kolasu.model

import com.badoo.reaktive.disposable.Disposable
import com.badoo.reaktive.observable.ObservableObserver
import com.badoo.reaktive.observable.filter
import com.badoo.reaktive.observable.map
import com.badoo.reaktive.subject.getObserver
import com.badoo.reaktive.subject.publish.PublishSubject
import com.strumenta.kolasu.language.Reference

/**
 * A reference associated by using a name.
 * It can be used only to refer to Nodes and not to other values.
 *
 * This is not statically enforced as we may want to use some interface, which cannot extend Node.
 * However, this is enforced dynamically.
 *
 * The node referenced by a ReferenceValue instance can be contained in the same AST or some external source
 * (typically other ASTs). In the latter case, we might want to indicate that, although we know which node the reference
 * is pointing to, we do not want to retrieve it straight away for performance reasons. In these circumstances,
 * the `referred` field is null and the `identifier` field is used instead. This, will be used to retrieve the
 * actual node at a later stage.
 */
class ReferenceValue<N>(
    var name: String,
    initialReferred: N? = null,
    var identifier: String? = null,
    var range: Range? = null,
) where N : PossiblyNamed {
    val changes = PublishSubject<ReferenceChangeNotification<N>>()

    var referred: N? = null
        set(value) {
            require(value is NodeLike || value == null) {
                "We cannot enforce it statically but only Node should be referred to. Instead $value was assigned"
            }
            changes.onNext(ReferenceChangeNotification(field, value as N?))
            field = value
        }

    private var container: NodeLike? = null
    private var reference: Reference? = null

    /**
     * When we instantiate the reference by name, we wired it so that changes to the reference by name will
     * be propagated to the owner of the reference, so that the observers of the owner will be notified
     * of changes in the reference.
     */
    fun setContainer(
        node: NodeLike,
        reference: Reference,
    ) {
        if (container == node) {
            return
        }
        if (container != null) {
            throw IllegalStateException("$this is already contained in $container as ${this.reference}")
        }
        changes
            .map {
                // When the reference is changed, we propagate it to the container
                ReferenceSet(node, reference, it.oldValue as NodeLike?, it.newValue as NodeLike?)
            }.subscribe(node.changes.getObserver { })
        changes
            .filter { it.oldValue != null }
            .map { ReferencedToRemoved(it.oldValue as NodeLike, reference, node) }
            .subscribe(
                object : ObservableObserver<ReferencedToRemoved<NodeLike>> {
                    override fun onComplete() {
                        TODO("Not yet implemented")
                    }

                    override fun onError(error: Throwable) {
                        TODO("Not yet implemented")
                    }

                    override fun onNext(value: ReferencedToRemoved<NodeLike>) {
                        value.node.changes.onNext(value)
                    }

                    override fun onSubscribe(disposable: Disposable) {
                    }
                },
            )
        changes
            .filter { it.newValue != null }
            .map { ReferencedToAdded(it.newValue as NodeLike, reference, node) }
            .subscribe(
                object : ObservableObserver<ReferencedToAdded<NodeLike>> {
                    override fun onComplete() {
                        TODO("Not yet implemented")
                    }

                    override fun onError(error: Throwable) {
                        TODO("Not yet implemented")
                    }

                    override fun onNext(value: ReferencedToAdded<NodeLike>) {
                        value.node.changes.onNext(value)
                    }

                    override fun onSubscribe(disposable: Disposable) {
                    }
                },
            )
        container = node
        this.reference = reference
    }

    init {
        this.referred = initialReferred
    }

    override fun toString(): String {
        return if (isResolved) {
            "Ref($name)[Solved]"
        } else {
            "Ref($name)[Unsolved]"
        }
    }

    val isResolved: Boolean
        get() = identifier != null || isRetrieved

    val isRetrieved: Boolean
        get() = referred != null

    override fun hashCode(): Int {
        return name.hashCode() * (1 + identifier.hashCode()) * (7 + if (isResolved) 2 else 1)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReferenceValue<*>) return false
        if (name != other.name) return false
        if (identifier != other.identifier) return false
        if (referred != other.referred) return false
        return true
    }
}
