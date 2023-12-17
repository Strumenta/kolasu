package com.strumenta.kolasu.serialization

import com.strumenta.kolasu.model.INode
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.traversing.ASTWalker
import com.strumenta.kolasu.traversing.walk
import java.util.IdentityHashMap

fun interface IdProvider {
    fun getId(node: INode): String?
}

class SequentialIdProvider(private var counter: Int = 0) : IdProvider {
    override fun getId(node: INode): String? {
        return "${this.counter++}"
    }
}

class OnlyReferencedIdProvider(
    private val root: INode,
    private var idProvider: IdProvider = SequentialIdProvider()
) : IdProvider {

    private val referencedElements: Set<PossiblyNamed> by lazy {
        this.root.walk().flatMap { node ->
            node.properties.filter { property -> property.value is ReferenceByName<*> }
                .mapNotNull { property -> (property.value as ReferenceByName<*>).referred }
        }.toSet()
    }

    override fun getId(node: INode): String? {
        return this.referencedElements.find { referencedElement -> referencedElement == node }
            ?.let { idProvider.getId(node) }
    }
}

/**
 * @param walker a function that generates a sequence of nodes. By default this is the depth-first "walk" method.
 * @param idProvider a provider that takes a node and returns a String identifier representing it
 * For post-order traversal, take "walkLeavesFirst".
 * @return walks the whole AST starting from the given node and associates each visited node with a generated id
 **/
fun INode.computeIds(
    walker: ASTWalker = INode::walk,
    idProvider: IdProvider = SequentialIdProvider()
): IdentityHashMap<INode, String> {
    val idsMap = IdentityHashMap<INode, String>()
    walker.invoke(this).forEach {
        val id = idProvider.getId(it)
        if (id != null) idsMap[it] = id
    }
    return idsMap
}

fun INode.computeIdsForReferencedNodes(
    walker: ASTWalker = INode::walk,
    idProvider: IdProvider = OnlyReferencedIdProvider(this)
): IdentityHashMap<INode, String> = computeIds(walker, idProvider)
