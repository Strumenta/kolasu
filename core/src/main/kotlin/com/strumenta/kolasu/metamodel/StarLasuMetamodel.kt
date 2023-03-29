package com.strumenta.kolasu.metamodel

import com.strumenta.kolasu.model.*
import com.strumenta.kolasu.model.lionweb.recordConceptForClass
import com.strumenta.kolasu.model.lionweb.recordConceptInterfaceForClass
import org.lionweb.lioncore.java.metamodel.Concept
import org.lionweb.lioncore.java.metamodel.ConceptInterface
import org.lionweb.lioncore.java.metamodel.Feature
import org.lionweb.lioncore.java.metamodel.LionCoreBuiltins
import org.lionweb.lioncore.java.metamodel.Metamodel
import org.lionweb.lioncore.java.metamodel.PrimitiveType
import org.lionweb.lioncore.java.metamodel.Property

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
        possiblyNamed.addFeature(Property.createOptional("name", LionCoreBuiltins.getString()))

        recordConceptForClass(ASTNode::class.java, astNode)
        recordConceptForClass(GenericErrorNode::class.java, genericErrorNode)
        recordConceptInterfaceForClass(Named::class.java, named)
        recordConceptInterfaceForClass(PossiblyNamed::class.java, possiblyNamed)
    }
}
