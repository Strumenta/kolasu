package com.strumenta.kolasu.model

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

data class ReferenceChangeNotification<N : PossiblyNamed>(
    val oldValue: N?,
    val newValue: N?,
)
