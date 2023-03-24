package com.strumenta.kolasu.metamodel

import org.lionweb.lioncore.java.metamodel.Concept
import org.lionweb.lioncore.java.metamodel.DataType
import org.lionweb.lioncore.java.metamodel.Metamodel
import org.lionweb.lioncore.java.metamodel.PrimitiveType

object StarLasuMetamodel : Metamodel() {
    val astNode: Concept
    val position: PrimitiveType
    init {
        astNode = Concept(this, "ASTNode", "StarLasu-ASTNode")
        position = PrimitiveType(this, "Position", "StarLasu-Position")
    }
}
