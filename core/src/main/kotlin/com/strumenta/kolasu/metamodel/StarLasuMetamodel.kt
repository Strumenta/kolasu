package com.strumenta.kolasu.metamodel

import com.strumenta.kolasu.model.*
import com.strumenta.kolasu.model.lionweb.recordConceptForClass
import com.strumenta.kolasu.model.lionweb.recordConceptInterfaceForClass
import org.lionweb.lioncore.java.metamodel.Concept
import org.lionweb.lioncore.java.metamodel.ConceptInterface
import org.lionweb.lioncore.java.metamodel.FeaturesContainer
import org.lionweb.lioncore.java.metamodel.LionCoreBuiltins
import org.lionweb.lioncore.java.metamodel.Metamodel
import org.lionweb.lioncore.java.metamodel.PrimitiveType
import org.lionweb.lioncore.java.metamodel.Property
import org.lionweb.lioncore.java.utils.MetamodelValidator

object StarLasuMetamodel : Metamodel() {
    val astNode: Concept
    val genericErrorNode: Concept
    val named: ConceptInterface
    val possiblyNamed: ConceptInterface
    val position: PrimitiveType
    val char: PrimitiveType
    init {
        this.name = "com.strumenta.StarLasu"
        this.id = this.name.replace('.', '_')
        this.key = this.id
        this.version = "1"

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

        elements.forEach { me ->
            me.key = me.id
            if (me is FeaturesContainer<*>) {
                me.features.forEach { f ->
                    f.id = me.id + "-" + f.name
                    f.key = f.id
                }
            }
        }

        val result = MetamodelValidator().validate(this)
        if (!result.isSuccessful) {
            throw RuntimeException("The StarLasu Metamodel is not valid: $result")
        }
    }
}
