package com.strumenta.kolasu.metamodel

import org.lionweb.lioncore.java.metamodel.Concept
import org.lionweb.lioncore.java.metamodel.ConceptInterface
import org.lionweb.lioncore.java.metamodel.Metamodel
import org.lionweb.lioncore.java.metamodel.PrimitiveType

object StarLasuMetamodel : Metamodel() {
    val astNode: Concept
    val genericErrorNode: Concept
    val named: ConceptInterface
    val possiblyNamed: ConceptInterface
    val position: PrimitiveType
    val char: PrimitiveType
    init {
        astNode = Concept(this, "ASTNode", "StarLasu-ASTNode")
        genericErrorNode = Concept(this, "GenericErrorNode", "StarLasu-GenericErrorNode")
        named = ConceptInterface(this, "Named", "StarLasu-Named")
        possiblyNamed = ConceptInterface(this, "PossiblyNamed", "StarLasu-PossiblyNamed")
        position = PrimitiveType(this, "Position", "StarLasu-Position")
        char = PrimitiveType(this, "Char", "StarLasu-Char")

        named.addExtendedInterface(possiblyNamed)
    }
}
