package com.strumenta.kolasu.metamodel

import org.lionweb.lioncore.java.metamodel.Concept
import org.lionweb.lioncore.java.metamodel.Metamodel

object StarLasuMetamodel : Metamodel() {
    val astNode: Concept
    init {
        astNode = Concept(this, "ASTNode", "StarLasu-ASTNode")
    }
}
