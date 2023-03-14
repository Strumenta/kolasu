package com.strumenta.kolasu.metamodel

import com.strumenta.kolasu.model.ASTNode
import org.lionweb.lioncore.java.metamodel.Concept
import org.lionweb.lioncore.java.metamodel.Metamodel
import org.lionweb.lioncore.java.metamodel.Property

object StarLasuMetamodel : Metamodel() {
    val astNode : Concept
    init {
        astNode = Concept(this, "ASTNode", "StarLasu-ASTNode")
    }
}