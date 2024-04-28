package com.strumenta.kolasu.serialization

import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceValue
import com.strumenta.kolasu.traversing.ASTWalker
import com.strumenta.kolasu.traversing.walk
import java.util.IdentityHashMap

fun interface IdProvider {
    fun getId(node: NodeLike): String?
}

class SequentialIdProvider(
    private var counter: Int = 0,
) : IdProvider {
    override fun getId(node: NodeLike): String? {
        return "${this.counter++}"
    }
}

class OnlyReferencedIdProvider(
    private val root: NodeLike,
    private var idProvider: IdProvider = SequentialIdProvider(),
) : IdProvider {
    private val referencedElements: Set<PossiblyNamed> by lazy {
        this
            .root
            .walk()
            .flatMap { node ->
                node.concept.allReferences.mapNotNull { (it.value(node) as ReferenceValue<*>).referred }
            }.toSet()
    }

    override fun getId(node: NodeLike): String? {
        return this
            .referencedElements
            .find { referencedElement -> referencedElement == node }
            ?.let { idProvider.getId(node) }
    }
}

/**
 * @param walker a function that generates a sequence of nodes. By default this is the depth-first "walk" method.
 * @param idProvider a provider that takes a node and returns a String identifier representing it
 * For post-order traversal, take "walkLeavesFirst".
 * @return walks the whole AST starting from the given node and associates each visited node with a generated id
 **/
fun NodeLike.computeIds(
    walker: ASTWalker = NodeLike::walk,
    idProvider: IdProvider = SequentialIdProvider(),
): IdentityHashMap<NodeLike, String> {
    val idsMap = IdentityHashMap<NodeLike, String>()
    walker.invoke(this).forEach {
        val id = idProvider.getId(it)
        if (id != null) idsMap[it] = id
    }
    return idsMap
}

fun NodeLike.computeIdsForReferencedNodes(
    walker: ASTWalker = NodeLike::walk,
    idProvider: IdProvider = OnlyReferencedIdProvider(this),
): IdentityHashMap<NodeLike, String> = computeIds(walker, idProvider)
