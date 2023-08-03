package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.language.Attribute
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.language.Reference
import com.strumenta.kolasu.model.Multiplicity
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.features
import com.strumenta.kolasu.model.isConcept
import com.strumenta.kolasu.model.isConceptInterface
import com.strumenta.kolasu.model.isMarkedAsNodeType
import io.lionweb.lioncore.java.language.Classifier
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.ConceptInterface
import io.lionweb.lioncore.java.language.DataType
import io.lionweb.lioncore.java.language.Enumeration
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.language.LionCoreBuiltins
import io.lionweb.lioncore.java.language.Property
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType

/**
 * This class is able to convert between Kolasu and LionWeb languages, tracking the mapping.
 */
class LionWebLanguageConverter {
    private val astClassesAndClassifiers = BiMap<KClass<*>, Classifier<*>>()
    private val classesAndEnumerations = BiMap<KClass<out Enum<*>>, Enumeration>()
    private val languages = BiMap<KolasuLanguage, Language>()

    init {
        val starLasuKLanguage = KolasuLanguage(StarLasuLWLanguage.name)
        languages.associate(starLasuKLanguage, StarLasuLWLanguage)
        registerMapping(Node::class, StarLasuLWLanguage.ASTNode)
        registerMapping(Named::class, StarLasuLWLanguage.Named)
    }

    fun getKolasuClassesToConceptsMapping(): Map<KClass<*>, Classifier<*>> {
        return astClassesAndClassifiers.asToBsMap
    }

    fun getConceptsToKolasuClassesMapping(): Map<Classifier<*>, KClass<*>> {
        return astClassesAndClassifiers.bsToAsMap
    }

    fun getEnumerationsToKolasuClassesMapping(): Map<Enumeration, KClass<*>> {
        return classesAndEnumerations.bsToAsMap
    }

    fun knownLWLanguages(): Set<Language> {
        return languages.bs
    }

    fun correspondingLanguage(kolasuLanguage: KolasuLanguage): Language {
        return languages.byA(kolasuLanguage)
            ?: throw java.lang.IllegalArgumentException("Unknown Kolasu Language $kolasuLanguage")
    }

    fun export(kolasuLanguage: KolasuLanguage): Language {
        val lionwebLanguage = Language()
        lionwebLanguage.version = "1"
        lionwebLanguage.name = kolasuLanguage.qualifiedName
        lionwebLanguage.key = kolasuLanguage.qualifiedName.replace('.', '-')
        lionwebLanguage.id = "starlasu_language_${kolasuLanguage.qualifiedName.replace('.', '-')}"
        lionwebLanguage.addDependency(StarLasuLWLanguage)

        // First we create all types
        kolasuLanguage.astClasses.forEach { astClass ->
            if (astClass.isConcept) {
                val concept = Concept(lionwebLanguage, astClass.simpleName)
                concept.key = lionwebLanguage.key + "_" + concept.name
                concept.id = lionwebLanguage.id + "_" + concept.name
                concept.isAbstract = astClass.isAbstract || astClass.isSealed
                registerMapping(astClass, concept)
            } else if (astClass.isConceptInterface) {
                val conceptInterface = ConceptInterface(lionwebLanguage, astClass.simpleName)
                conceptInterface.key = lionwebLanguage.key + "_" + conceptInterface.name
                conceptInterface.id = lionwebLanguage.id + "_" + conceptInterface.name
                registerMapping(astClass, conceptInterface)
            }
        }

        // Then we populate them, so that self-references can be described
        kolasuLanguage.astClasses.forEach { astClass ->
            val featuresContainer = astClassesAndClassifiers.byA(astClass)

            if (astClass.java.isInterface) {
                val conceptInterface = featuresContainer as ConceptInterface
                val superInterfaces = astClass.supertypes.map { it.classifier as KClass<*> }
                    .filter { it.java.isInterface }
                superInterfaces.filter { it.isMarkedAsNodeType() }.forEach {
                    conceptInterface.addExtendedInterface(toConceptInterface(it))
                }
            } else {
                val concept = featuresContainer as Concept
                val superClasses = astClass.supertypes.map { it.classifier as KClass<*> }
                    .filter { !it.java.isInterface }
                if (superClasses.size == 1) {
                    concept.extendedConcept = astClassesAndClassifiers.byA(superClasses.first()) as Concept
                } else {
                    throw IllegalStateException()
                }
                val interfaces = astClass.supertypes.map { it.classifier as KClass<*> }.filter { it.java.isInterface }
                interfaces.filter { it.isMarkedAsNodeType() }.forEach {
                    concept.addImplementedInterface(toConceptInterface(it))
                }
            }
            astClass.features().forEach {
                when (it) {
                    is Attribute -> {
                        val prop = Property(it.name, featuresContainer)
                        prop.key = featuresContainer.key + "_" + prop.name
                        prop.id = featuresContainer.id + "_" + prop.name
                        prop.setOptional(it.optional)
                        prop.setType(toLWDataType(it.type, lionwebLanguage))
                        featuresContainer.addFeature(prop)
                    }
                    is Reference -> {
                        val ref = io.lionweb.lioncore.java.language.Reference(it.name, featuresContainer)
                        ref.key = featuresContainer.key + "_" + ref.name
                        ref.id = featuresContainer.id + "_" + ref.name
                        ref.setOptional(it.optional)
                        ref.setType(toLWClassifier(it.type))
                        featuresContainer.addFeature(ref)
                    }
                    is Containment -> {
                        val cont = io.lionweb.lioncore.java.language.Containment(it.name, featuresContainer)
                        cont.key = featuresContainer.key + "_" + cont.name
                        cont.id = featuresContainer.id + "_" + cont.name
                        cont.setOptional(true)
                        cont.setMultiple(it.multiplicity == Multiplicity.MANY)
                        cont.setType(toLWClassifier(it.type))
                        featuresContainer.addFeature(cont)
                    }
                }
            }
        }
        languages.associate(kolasuLanguage, lionwebLanguage)
        return lionwebLanguage
    }

    private fun toLWDataType(kType: KType, lionwebLanguage: Language): DataType<*> {
        return when (kType) {
            Int::class.createType() -> LionCoreBuiltins.getInteger()
            Long::class.createType() -> LionCoreBuiltins.getInteger()
            String::class.createType() -> LionCoreBuiltins.getString()
            Boolean::class.createType() -> LionCoreBuiltins.getBoolean()
            else -> {
                val kClass = kType.classifier as KClass<*>
                val isEnum = kClass.supertypes.any { it.classifier == Enum::class }
                if (isEnum) {
                    val enumeration = classesAndEnumerations.byA(kClass as KClass<out Enum<*>>)
                    if (enumeration == null) {
                        val newEnumeration = Enumeration(lionwebLanguage, kClass.simpleName)
                        lionwebLanguage.addElement(newEnumeration)
                        classesAndEnumerations.associate(kClass, newEnumeration)
                        return newEnumeration
                    } else {
                        return enumeration
                    }
                } else {
                    throw UnsupportedOperationException("KType: $kType")
                }
            }
        }
    }

    fun toConceptInterface(kClass: KClass<*>): ConceptInterface {
        return toLWClassifier(kClass) as ConceptInterface
    }

    fun toConcept(kClass: KClass<*>): Concept {
        return toLWClassifier(kClass) as Concept
    }

    fun matchingKClass(concept: Concept): KClass<*>? {
        return this.astClassesAndClassifiers.bsToAsMap.entries.find {
            it.key.key == concept.key &&
                it.key.language!!.id == concept.language!!.id &&
                it.key.language!!.version == concept.language!!.version
        }?.value
    }

    fun importLanguages(lwLanguage: Language, kolasuLanguage: KolasuLanguage) {
        this.languages.associate(kolasuLanguage, lwLanguage)
        kolasuLanguage.astClasses.forEach { astClass ->
            var classifier: Classifier<*>? = null
            val annotation = astClass.annotations.filterIsInstance(LionWebAssociation::class.java).firstOrNull()
            if (annotation != null) {
                classifier = lwLanguage.elements.filterIsInstance(Classifier::class.java).find {
                    it.key == annotation.key
                }
            }
            if (classifier != null) {
                registerMapping(astClass, classifier)
            }
        }
        kolasuLanguage.enumClasses.forEach { enumClass ->
            var enumeration: Enumeration? = null
            val annotation = enumClass.annotations.filterIsInstance(LionWebAssociation::class.java).firstOrNull()
            if (annotation != null) {
                enumeration = lwLanguage.elements.filterIsInstance(Enumeration::class.java).find {
                    it.key == annotation.key
                }
            }
            if (enumeration != null) {
                classesAndEnumerations.associate(enumClass, enumeration)
            }
        }
    }

    private fun registerMapping(kolasuClass: KClass<*>, featuresContainer: Classifier<*>) {
        astClassesAndClassifiers.associate(kolasuClass, featuresContainer)
    }

    private fun toLWClassifier(kClass: KClass<*>): Classifier<*> {
        return astClassesAndClassifiers.byA(kClass) ?: throw IllegalArgumentException("Unknown KClass $kClass")
    }
}
