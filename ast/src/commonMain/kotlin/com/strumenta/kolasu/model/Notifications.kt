package com.strumenta.kolasu.model

import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.Property
import com.strumenta.kolasu.language.Reference

sealed class NodeNotification<N : NodeLike> {
    abstract val node: N
}

data class PropertyChangedNotification<N : NodeLike, V : Any?>(
    override val node: N,
    val property: Property,
    val oldValue: V,
    val newValue: V,
) : NodeNotification<N>()

data class ChildAdded<N : NodeLike>(
    override val node: N,
    val containment: Containment,
    val child: NodeLike,
) : NodeNotification<N>()

data class ChildRemoved<N : NodeLike>(
    override val node: N,
    val containment: Containment,
    val child: NodeLike,
) : NodeNotification<N>()

data class ReferenceSet<N : NodeLike>(
    override val node: N,
    val reference: Reference,
    val oldReferredNode: NodeLike?,
    val newReferredNode: NodeLike?,
) : NodeNotification<N>()

data class ReferencedToAdded<N : NodeLike>(
    override val node: N,
    val reference: Reference,
    val referringNode: NodeLike,
) : NodeNotification<N>()

data class ReferencedToRemoved<N : NodeLike>(
    override val node: N,
    val reference: Reference,
    val referringNode: NodeLike,
) : NodeNotification<N>()

data class ReferenceChangeNotification<N : NodeLike>(
    val oldValue: N?,
    val newValue: N?,
)
