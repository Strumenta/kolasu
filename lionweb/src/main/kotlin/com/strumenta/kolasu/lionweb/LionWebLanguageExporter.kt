package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.language.Attribute
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.Reference
import com.strumenta.kolasu.model.Multiplicity
import io.lionweb.lioncore.java.language.Language
import kotlin.reflect.KClass
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.containedType
import com.strumenta.kolasu.model.features
import com.strumenta.kolasu.model.isConcept
import com.strumenta.kolasu.model.isConceptInterface
import com.strumenta.kolasu.model.isContainment
import com.strumenta.kolasu.model.isReference
import com.strumenta.kolasu.model.nodeProperties
import com.strumenta.kolasu.model.referredType
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.ConceptInterface
import io.lionweb.lioncore.java.language.DataType
import io.lionweb.lioncore.java.language.FeaturesContainer
import io.lionweb.lioncore.java.language.LionCoreBuiltins
import io.lionweb.lioncore.java.language.Property
import kotlin.reflect.KType
import kotlin.reflect.full.createType

/**
 * There is no explicit Language defined in Kolasu, it is just a bunch of AST classes.
 * We create this Class to represent that collection of AST classes.
 */
class KolasuLanguage {
    val astClasses: MutableList<KClass<out Node>> = mutableListOf()

    fun <N: Node>addClass(kClass: KClass<N>) : Boolean {
        if (!astClasses.contains(kClass) && astClasses.add(kClass)) {
            if (kClass.isSealed) {
                kClass.sealedSubclasses.forEach {
                    addClass(it)
                }
            }
            kClass.nodeProperties.forEach { nodeProperty ->
                if (nodeProperty.isContainment()) {
                    addClass(nodeProperty.containedType())
                } else if (nodeProperty.isReference()) {
                    addClass(nodeProperty.referredType())
                }
                // TODO add enums and other datatypes
            }
            return true
        } else {
            return false
        }
    }
}

class LionWebLanguageExporter {

    private val astToLWConcept = mutableMapOf<KClass<out Node>, FeaturesContainer<*>>()

    fun export(kolasuLanguage: KolasuLanguage) : Language {
        val lionwebLanguage = Language()
        // First we create all types
        kolasuLanguage.astClasses.forEach { astClass ->
            if (astClass.isConcept) {
                val concept = Concept(lionwebLanguage, astClass.simpleName)
                astToLWConcept[astClass] = concept
            } else if (astClass.isConceptInterface) {
                val conceptInterface = ConceptInterface(lionwebLanguage, astClass.simpleName)
                astToLWConcept[astClass] = conceptInterface
            }
        }
        // Then we populate them, so that self-references can be described
        kolasuLanguage.astClasses.forEach { astClass ->
            val featuresContainer = astToLWConcept[astClass]!!
            astClass.features().forEach {
                when (it) {
                    is Attribute -> {
                        val prop = Property(it.name, featuresContainer)
                        prop.setOptional(it.optional)
                        prop.setType(toLWDataType(it.type))
                        featuresContainer.addFeature(prop)
                    }
                    is Reference -> {
                        val ref = io.lionweb.lioncore.java.language.Reference(it.name, featuresContainer)
                        ref.setOptional(it.optional)
                        featuresContainer.addFeature(ref)
                    }
                    is Containment -> {
                        val cont = io.lionweb.lioncore.java.language.Containment(it.name, featuresContainer)
                        cont.setOptional(true)
                        cont.setMultiple(it.multiplicity == Multiplicity.MANY)
                        cont.setType(toLWFeaturesContainer(it.type))
                        featuresContainer.addFeature(cont)
                    }
                }
            }
        }
        return lionwebLanguage
    }

    private fun toLWDataType(kType: KType) : DataType<*> {
        return when (kType) {
            Int::class.createType() -> LionCoreBuiltins.getInteger()
            String::class.createType() -> LionCoreBuiltins.getString()
            else -> throw UnsupportedOperationException("KType: $kType")
        }
    }

    private fun toLWFeaturesContainer(kClass: KClass<*>) : FeaturesContainer<*> {
        return astToLWConcept[kClass] ?: throw IllegalArgumentException("Unknown KClass $kClass")
    }

}

