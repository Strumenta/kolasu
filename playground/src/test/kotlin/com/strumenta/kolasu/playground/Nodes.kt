package com.strumenta.kolasu.playground

import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.language.explore
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node

object StarLasuLanguageInstance : StarLasuLanguage("com.strumenta.kolasu.playground") {
    init {
        explore(ANode::class, BNode::class)
    }
}

data class ANode(
    override val name: String,
    val value: Int,
) : Node(),
    Named

data class BNode(
    override val name: String,
    val value: Int,
) : Node(),
    Named
