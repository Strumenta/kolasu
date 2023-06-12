package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.language.Attribute
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.Reference
import com.strumenta.kolasu.model.Multiplicity
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.features
import com.strumenta.kolasu.model.isConcept
import com.strumenta.kolasu.model.isConceptInterface
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.ConceptInterface
import io.lionweb.lioncore.java.language.DataType
import io.lionweb.lioncore.java.language.FeaturesContainer
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.language.LionCoreBuiltins
import io.lionweb.lioncore.java.language.Property
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType

class LionWebLanguageExporter {

    private val astToLWConcept = mutableMapOf<KClass<*>, FeaturesContainer<*>>()
    private val LWConceptToKolasuClass = mutableMapOf<FeaturesContainer<*>, KClass<*>>()
    private val kLanguageToLWLanguage = mutableMapOf<KolasuLanguage, Language>()

    init {
        val starLasuKLanguage = KolasuLanguage("com.strumenta.starlasu")
        kLanguageToLWLanguage[starLasuKLanguage] = StarLasuLWLanguage
        registerMapping(Node::class, StarLasuLWLanguage.ASTNode)
        registerMapping(Named::class, StarLasuLWLanguage.Named)
    }

    private fun registerMapping(kolasuClass: KClass<*>, featuresContainer: FeaturesContainer<*>) {
        astToLWConcept[kolasuClass] = featuresContainer
        LWConceptToKolasuClass[featuresContainer] = kolasuClass
    }

    fun getKolasuClassesToConceptsMapping(): Map<KClass<*>, FeaturesContainer<*>> {
        return astToLWConcept
    }

    fun getConceptsToKolasuClassesMapping(): Map<FeaturesContainer<*>, KClass<*>> {
        return LWConceptToKolasuClass
    }

    fun knownLWLanguages(): Set<Language> {
        return kLanguageToLWLanguage.values.toSet()
    }

    fun correspondingLanguage(kolasuLanguage: KolasuLanguage): Language {
        return kLanguageToLWLanguage[kolasuLanguage]
            ?: throw java.lang.IllegalArgumentException("Unknown Kolasu Language $kolasuLanguage")
    }

    fun export(kolasuLanguage: KolasuLanguage): Language {
        val lionwebLanguage = Language()
        lionwebLanguage.version = "1"
        lionwebLanguage.name = kolasuLanguage.qualifiedName
        lionwebLanguage.key = kolasuLanguage.qualifiedName

        // First we create all types
        kolasuLanguage.astClasses.forEach { astClass ->
            if (astClass.isConcept) {
                val concept = Concept(lionwebLanguage, astClass.simpleName)
                concept.key = lionwebLanguage.key + "-" + concept.name
                concept.isAbstract = astClass.isAbstract || astClass.isSealed
                registerMapping(astClass, concept)
            } else if (astClass.isConceptInterface) {
                val conceptInterface = ConceptInterface(lionwebLanguage, astClass.simpleName)
                conceptInterface.key = lionwebLanguage.key + "-" + conceptInterface.name
                registerMapping(astClass, conceptInterface)
            }
        }
        // Then we populate them, so that self-references can be described
        kolasuLanguage.astClasses.forEach { astClass ->
            val featuresContainer = astToLWConcept[astClass]!!

            if (astClass.java.isInterface) {
                TODO()
            } else {
                val concept = featuresContainer as Concept
                val superClasses = astClass.supertypes.map { it.classifier as KClass<*> }
                    .filter { !it.java.isInterface }
                if (superClasses.size == 1) {
                    concept.extendedConcept = astToLWConcept[superClasses.first()] as Concept
                } else {
                    throw IllegalStateException()
                }
                val interfaces = astClass.supertypes.map { it.classifier as KClass<*> }.filter { it.java.isInterface }
                interfaces.forEach {
                    concept.addImplementedInterface(toConceptInterface(it))
                }
            }
            astClass.features().forEach {
                when (it) {
                    is Attribute -> {
                        val prop = Property(it.name, featuresContainer)
                        prop.key = prop.name
                        prop.setOptional(it.optional)
                        prop.setType(toLWDataType(it.type))
                        featuresContainer.addFeature(prop)
                    }
                    is Reference -> {
                        val ref = io.lionweb.lioncore.java.language.Reference(it.name, featuresContainer)
                        ref.key = ref.name
                        ref.setOptional(it.optional)
                        ref.setType(toLWFeaturesContainer(it.type))
                        featuresContainer.addFeature(ref)
                    }
                    is Containment -> {
                        val cont = io.lionweb.lioncore.java.language.Containment(it.name, featuresContainer)
                        cont.key = cont.name
                        cont.setOptional(true)
                        cont.setMultiple(it.multiplicity == Multiplicity.MANY)
                        cont.setType(toLWFeaturesContainer(it.type))
                        featuresContainer.addFeature(cont)
                    }
                }
            }
        }
        kLanguageToLWLanguage[kolasuLanguage] = lionwebLanguage
        return lionwebLanguage
    }

    private fun toLWDataType(kType: KType): DataType<*> {
        return when (kType) {
            Int::class.createType() -> LionCoreBuiltins.getInteger()
            String::class.createType() -> LionCoreBuiltins.getString()
            else -> throw UnsupportedOperationException("KType: $kType")
        }
    }

    private fun toLWFeaturesContainer(kClass: KClass<*>): FeaturesContainer<*> {
        return astToLWConcept[kClass] ?: throw IllegalArgumentException("Unknown KClass $kClass")
    }

    fun toConceptInterface(kClass: KClass<*>): ConceptInterface {
        return toLWFeaturesContainer(kClass) as ConceptInterface
    }

    fun toConcept(kClass: KClass<*>): Concept {
        return toLWFeaturesContainer(kClass) as Concept
    }
}
