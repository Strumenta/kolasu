package com.strumenta.kolasu.lionwebclient

import com.strumenta.kolasu.lionweb.KNode
import com.strumenta.kolasu.lionweb.LWNode
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Source
import com.strumenta.starlasu.base.ASTLanguage
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.model.HasSettableParent
import io.lionweb.lioncore.java.model.impl.ProxyNode
import io.lionweb.lioncore.kotlin.children

fun Node.withSource(source: Source): Node {
    this.setSourceForTree(source)
    require(this.source === source)
    return this
}

fun HasSettableParent.setParentID(parentID: String?) {
    val parent =
        if (parentID == null) {
            null
        } else {
            ProxyNode(parentID)
        }
    this.setParent(parent)
}

fun KolasuClient.getASTRoots(aLWNode: LWNode): Sequence<KNode> {
    val res = mutableListOf<KNode>()

    fun exploreForASTs(aLWNode: LWNode) {
        val isKNode: Boolean = isKolasuConcept(aLWNode.classifier)
        if (isKNode) {
            res.add(toKolasuNode(aLWNode))
        } else {
            aLWNode.children.forEach { exploreForASTs(it) }
        }
    }

    exploreForASTs(aLWNode)
    return res.asSequence()
}

fun isKolasuConcept(concept: Concept): Boolean {
    return concept.allAncestors().contains(ASTLanguage.getASTNode())
}
