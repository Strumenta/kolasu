package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.language.Annotation
import com.strumenta.kolasu.language.BaseStarLasuLanguage
import com.strumenta.kolasu.language.ConceptInterface
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.EnumType
import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.language.booleanType
import com.strumenta.kolasu.language.intType
import com.strumenta.kolasu.language.stringType
import com.strumenta.kolasu.model.BehaviorDeclaration
import com.strumenta.kolasu.model.CommonElement
import com.strumenta.kolasu.model.Documentation
import com.strumenta.kolasu.model.EntityDeclaration
import com.strumenta.kolasu.model.EntityGroupDeclaration
import com.strumenta.kolasu.model.Expression
import com.strumenta.kolasu.model.Multiplicity
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.Parameter
import com.strumenta.kolasu.model.PlaceholderElement
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.Statement
import com.strumenta.kolasu.model.TypeAnnotation
import com.strumenta.kolasu.model.declaredFeatures
import com.strumenta.kolasu.model.isConcept
import com.strumenta.kolasu.model.isConceptInterface
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.DataType
import io.lionweb.lioncore.java.language.Enumeration
import io.lionweb.lioncore.java.language.EnumerationLiteral
import io.lionweb.lioncore.java.language.Interface
import io.lionweb.lioncore.java.language.LionCoreBuiltins
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import com.strumenta.kolasu.language.Classifier as KClassifier
import com.strumenta.kolasu.language.Concept as KConcept
import com.strumenta.kolasu.language.ConceptInterface as KConceptInterface
import com.strumenta.kolasu.language.DataType as KDataType
import com.strumenta.kolasu.language.PrimitiveType as KPrimitiveType
import com.strumenta.kolasu.language.Property as KProperty
import com.strumenta.kolasu.language.Reference as KReference
import io.lionweb.lioncore.java.language.Classifier as LWClassifier
import io.lionweb.lioncore.java.language.PrimitiveType as LWPrimitiveType
import io.lionweb.lioncore.java.language.Property as LWProperty
import io.lionweb.lioncore.java.language.Reference as LWReference

/**
 * This class is able to convert between Kolasu and LionWeb languages, tracking the mapping.
 */
class LionWebLanguageConverter {
    @Deprecated("Use kConceptsAndLWConcepts")
    private val astClassesAndClassifiers = BiMap<KClass<*>, LWClassifier<*>>()

    @Deprecated("Use kEnumerationAndLWEnumerations")
    private val classesAndEnumerations = BiMap<EnumKClass, Enumeration>()

    @Deprecated("Use primitiveTypesMapping")
    private val classesAndPrimitiveTypes = BiMap<KClass<*>, LWPrimitiveType>()

    @Deprecated("Use sLanguagesMapping")
    private val kLanguagesMapping = BiMap<KolasuLanguage, LWLanguage>()

    private val kConceptsAndLWConcepts = BiMap<KClassifier, LWClassifier<*>>()
    private val primitiveTypesMapping = BiMap<KPrimitiveType, LWPrimitiveType>()
    private val kEnumerationAndLWEnumerations = BiMap<EnumType, Enumeration>()
    private val sLanguagesMapping = BiMap<StarLasuLanguage, LWLanguage>()

    init {
        val starLasuKLanguage = KolasuLanguage(StarLasuLWLanguage.name)
        kLanguagesMapping.associate(starLasuKLanguage, StarLasuLWLanguage)
        registerMapping(NodeLike::class, StarLasuLWLanguage.ASTNode)
        registerMapping(Node::class, StarLasuLWLanguage.ASTNode)
        registerMapping(Named::class, LionCoreBuiltins.getINamed())
        registerMapping(PossiblyNamed::class, LionCoreBuiltins.getINamed())

        registerMapping(BaseStarLasuLanguage.iNamed, LionCoreBuiltins.getINamed())
        registerMapping(BaseStarLasuLanguage.iPossiblyNamed, LionCoreBuiltins.getINamed())
        registerMapping(CommonElement::class, StarLasuLWLanguage.CommonElement)
        registerMapping(BehaviorDeclaration::class, StarLasuLWLanguage.BehaviorDeclaration)
        registerMapping(Documentation::class, StarLasuLWLanguage.Documentation)
        registerMapping(EntityDeclaration::class, StarLasuLWLanguage.EntityDeclaration)
        registerMapping(EntityGroupDeclaration::class, StarLasuLWLanguage.EntityGroupDeclaration)
        registerMapping(Expression::class, StarLasuLWLanguage.Expression)
        registerMapping(Parameter::class, StarLasuLWLanguage.Parameter)
        registerMapping(PlaceholderElement::class, StarLasuLWLanguage.PlaceholderElement)
        registerMapping(Statement::class, StarLasuLWLanguage.Statement)
        registerMapping(TypeAnnotation::class, StarLasuLWLanguage.TypeAnnotation)

        registerMapping(Issue::class, StarLasuLWLanguage.Issue)
        classesAndEnumerations.associate(
            IssueSeverity::class,
            (StarLasuLWLanguage.Issue.getFeatureByName(Issue::severity.name) as Property).type as Enumeration
        )
        classesAndEnumerations.associate(
            IssueType::class,
            (StarLasuLWLanguage.Issue.getFeatureByName(Issue::type.name) as Property).type as Enumeration
        )
        registerMapping(ParsingResult::class, StarLasuLWLanguage.ParsingResult)
    }

    fun exportToLionWeb(starLasuLanguage: StarLasuLanguage): LWLanguage {
        val lionwebLanguage = LWLanguage()
        lionwebLanguage.version = "1"
        lionwebLanguage.name = starLasuLanguage.qualifiedName
        lionwebLanguage.key = starLasuLanguage.qualifiedName.replace('.', '-')
        lionwebLanguage.id = "starlasu_language_${starLasuLanguage.qualifiedName.replace('.', '-')}"
        lionwebLanguage.addDependency(StarLasuLWLanguage)

        starLasuLanguage.enums.forEach { enumType ->
            toLWEnumeration(enumType, lionwebLanguage)
        }

        starLasuLanguage.primitives.forEach { primitiveType ->
            toLWPrimitiveType(primitiveType, lionwebLanguage)
        }

        // First we create all types
        starLasuLanguage.classifiers.forEach { conceptLike ->
            when (conceptLike) {
                is KConcept -> {
                    val concept = Concept(lionwebLanguage, conceptLike.name)
                    concept.extendedConcept = StarLasuLWLanguage.ASTNode
                    concept.isPartition = false
                    concept.key = lionwebLanguage.key + "_" + concept.name
                    concept.id = lionwebLanguage.id + "_" + concept.name
                    concept.isAbstract = conceptLike.isAbstract
                    registerMapping(conceptLike, concept)
                }
                is KConceptInterface -> {
                    val conceptInterface = Interface(lionwebLanguage, conceptLike.name)
                    conceptInterface.key = lionwebLanguage.key + "_" + conceptInterface.name
                    conceptInterface.id = lionwebLanguage.id + "_" + conceptInterface.name
                    registerMapping(conceptLike, conceptInterface)
                }
                else -> TODO()
            }
        }

        // Then we populate them, so that self-references can be described
        starLasuLanguage.classifiers.forEach { kConceptLike ->
            val featuresContainer = kConceptsAndLWConcepts.byA(kConceptLike)

            when (kConceptLike) {
                is KConceptInterface -> {
                    val lwConceptInterface = featuresContainer as Interface
                    kConceptLike.superInterfaces.forEach { kSuperInterface ->
                        lwConceptInterface.addExtendedInterface(correspondingInterface(kSuperInterface))
                    }
                }
                is KConcept -> {
                    val lwConcept = featuresContainer as Concept
                    if (kConceptLike.superConcept == null) {
                        lwConcept.extendedConcept = StarLasuLWLanguage.ASTNode
                    } else {
                        lwConcept.extendedConcept = kConceptsAndLWConcepts.byA(kConceptLike.superConcept!!) as Concept
                    }
                    kConceptLike.conceptInterfaces.forEach { kSuperInterface ->
                        lwConcept.addImplementedInterface(correspondingInterface(kSuperInterface))
                    }
                }
                else -> TODO()
            }
            kConceptLike.declaredFeatures.forEach {
                when (it) {
                    is KProperty -> {
                        val prop = LWProperty(it.name, featuresContainer)
                        prop.key = featuresContainer.key + "_" + prop.name
                        prop.id = featuresContainer.id + "_" + prop.name
                        prop.setOptional(it.optional)
                        prop.setType(toLWDataType(it.type, lionwebLanguage))
                        featuresContainer.addFeature(prop)
                    }

                    is KReference -> {
                        val ref =
                            LWReference(it.name, featuresContainer)
                        ref.key = featuresContainer.key + "_" + ref.name
                        ref.id = featuresContainer.id + "_" + ref.name
                        ref.setOptional(it.optional)
                        ref.setType(correspondingClassifier(it.type))
                        featuresContainer.addFeature(ref)
                    }

                    is Containment -> {
                        val cont =
                            io
                                .lionweb
                                .lioncore
                                .java
                                .language
                                .Containment(it.name, featuresContainer)
                        cont.key = featuresContainer.key + "_" + cont.name
                        cont.id = featuresContainer.id + "_" + cont.name
                        cont.setOptional(true)
                        cont.setMultiple(it.multiplicity == Multiplicity.MANY)
                        cont.setType(correspondingClassifier(it.type))
                        featuresContainer.addFeature(cont)
                    }
                }
            }
        }
        this.sLanguagesMapping.associate(starLasuLanguage, lionwebLanguage)
        return lionwebLanguage
    }

    @Deprecated("take a StarLasu Language instead")
    fun exportToLionWeb(kolasuLanguage: KolasuLanguage): LWLanguage {
        val lionwebLanguage = LWLanguage()
        lionwebLanguage.version = "1"
        lionwebLanguage.name = kolasuLanguage.qualifiedName
        lionwebLanguage.key = kolasuLanguage.qualifiedName.replace('.', '-')
        lionwebLanguage.id = "starlasu_language_${kolasuLanguage.qualifiedName.replace('.', '-')}"
        lionwebLanguage.addDependency(StarLasuLWLanguage)

        kolasuLanguage.enumClasses.forEach { enumClass ->
            toLWEnumeration(enumClass, lionwebLanguage)
        }

        kolasuLanguage.primitiveClasses.forEach { primitiveClass ->
            toLWPrimitiveType(primitiveClass, lionwebLanguage)
        }

        // First we create all types
        kolasuLanguage.astClasses.forEach { astClass ->
            if (astClass.isConcept) {
                val concept = Concept(lionwebLanguage, astClass.simpleName)
                concept.isPartition = false
                concept.key = lionwebLanguage.key + "_" + concept.name
                concept.id = lionwebLanguage.id + "_" + concept.name
                concept.isAbstract = astClass.isAbstract || astClass.isSealed
                registerMapping(astClass, concept)
            } else if (astClass.isConceptInterface) {
                val conceptInterface = Interface(lionwebLanguage, astClass.simpleName)
                conceptInterface.key = lionwebLanguage.key + "_" + conceptInterface.name
                conceptInterface.id = lionwebLanguage.id + "_" + conceptInterface.name
                registerMapping(astClass, conceptInterface)
            }
        }

        // Then we populate them, so that self-references can be described
        kolasuLanguage.astClasses.forEach { astClass ->
            val featuresContainer = astClassesAndClassifiers.byA(astClass)

            if (astClass.java.isInterface) {
                val conceptInterface = featuresContainer as Interface
                val superInterfaces =
                    astClass
                        .supertypes
                        .map { it.classifier as KClass<*> }
                        .filter { it.java.isInterface }
                superInterfaces.filter { it.isConceptInterface }.forEach {
                    conceptInterface.addExtendedInterface(correspondingInterface(it))
                }
            } else {
                val concept = featuresContainer as Concept
                val superClasses =
                    astClass
                        .supertypes
                        .map { it.classifier as KClass<*> }
                        .filter { !it.java.isInterface }
                if (superClasses.size == 1) {
                    concept.extendedConcept = astClassesAndClassifiers.byA(superClasses.first()) as Concept
                } else {
                    throw IllegalStateException()
                }
                val interfaces = astClass.supertypes.map { it.classifier as KClass<*> }.filter { it.java.isInterface }
                interfaces.filter { it.isConceptInterface }.forEach {
                    concept.addImplementedInterface(correspondingInterface(it))
                }
            }
            val features =
                try {
                    astClass.declaredFeatures()
                } catch (e: RuntimeException) {
                    throw RuntimeException("Issue processing features for AST class ${astClass.qualifiedName}", e)
                }

            features.forEach {
                when (it) {
                    is KProperty -> {
                        val prop = LWProperty(it.name, featuresContainer)
                        prop.key = featuresContainer.key + "_" + prop.name
                        prop.id = featuresContainer.id + "_" + prop.name
                        prop.setOptional(it.optional)
                        prop.setType(toLWDataType(it.type.kClass().createType(), lionwebLanguage))
                        featuresContainer.addFeature(prop)
                    }

                    is KReference -> {
                        val ref =
                            LWReference(it.name, featuresContainer)
                        ref.key = featuresContainer.key + "_" + ref.name
                        ref.id = featuresContainer.id + "_" + ref.name
                        ref.setOptional(it.optional)
                        // Here we deal with kclasses (not with the types), so we find the class from the type
                        ref.setType(toLWClassifier(it.type.kClass()))
                        featuresContainer.addFeature(ref)
                    }

                    is Containment -> {
                        val cont =
                            io
                                .lionweb
                                .lioncore
                                .java
                                .language
                                .Containment(it.name, featuresContainer)
                        cont.key = featuresContainer.key + "_" + cont.name
                        cont.id = featuresContainer.id + "_" + cont.name
                        cont.setOptional(true)
                        cont.setMultiple(it.multiplicity == Multiplicity.MANY)
                        // Here we deal with kclasses (not with the types), so we find the class from the type
                        cont.setType(toLWClassifier(it.type.kClass()))
                        featuresContainer.addFeature(cont)
                    }
                }
            }
        }
        this.kLanguagesMapping.associate(kolasuLanguage, lionwebLanguage)
        return lionwebLanguage
    }

    /**
     * Importing a LionWeb language as a Kolasu language requires the generation of classes, to be performed
     * separately. Once that is done we associate the Kolasu language defined by those classes to a certain
     * LionWeb language, so that we can import LionWeb models by instantiating the corresponding classes in the
     * Kolasu language.
     */
    fun associateLanguages(
        lwLanguage: LWLanguage,
        kolasuLanguage: KolasuLanguage,
    ) {
        this.kLanguagesMapping.associate(kolasuLanguage, lwLanguage)
        kolasuLanguage.astClasses.forEach { astClass ->
            var classifier: LWClassifier<*>? = null
            val annotation = astClass.annotations.filterIsInstance<LionWebAssociation>().firstOrNull()
            if (annotation != null) {
                classifier =
                    lwLanguage.elements.filterIsInstance(LWClassifier::class.java).find {
                        it.key == annotation.key
                    }
            }
            if (classifier != null) {
                registerMapping(astClass, classifier)
            }
        }
        kolasuLanguage.enumClasses.forEach { enumClass ->
            var enumeration: Enumeration? = null
            val annotation = enumClass.annotations.filterIsInstance<LionWebAssociation>().firstOrNull()
            if (annotation != null) {
                enumeration =
                    lwLanguage.elements.filterIsInstance<Enumeration>().find {
                        it.key == annotation.key
                    }
            }
            if (enumeration != null) {
                classesAndEnumerations.associate(enumClass, enumeration)
            }
        }
        kolasuLanguage.primitiveClasses.forEach { primitiveClass ->
            var primitiveType: LWPrimitiveType? = null
            val annotation = primitiveClass.annotations.filterIsInstance<LionWebAssociation>().firstOrNull()
            if (annotation != null) {
                primitiveType =
                    lwLanguage.elements.filterIsInstance<LWPrimitiveType>().find {
                        it.key == annotation.key
                    }
            }
            if (primitiveType != null) {
                classesAndPrimitiveTypes.associate(primitiveClass, primitiveType)
            }
        }
    }

    fun knownLWLanguages(): Set<LWLanguage> {
        return kLanguagesMapping.bs
    }

    fun knownKolasuLanguages(): Set<KolasuLanguage> {
        return kLanguagesMapping.`as`
    }

    fun correspondingLanguage(kolasuLanguage: KolasuLanguage): LWLanguage {
        return kLanguagesMapping.byA(kolasuLanguage)
            ?: throw java.lang.IllegalArgumentException("Unknown Kolasu Language $kolasuLanguage")
    }

    fun correspondingLanguage(lwLanguage: LWLanguage): KolasuLanguage {
        return kLanguagesMapping.byB(lwLanguage)
            ?: throw java.lang.IllegalArgumentException("Unknown LionWeb Language $lwLanguage")
    }

    fun getKolasuClassesToClassifiersMapping(): Map<KClass<*>, LWClassifier<*>> {
        return astClassesAndClassifiers.asToBsMap
    }

    fun getClassifiersToKolasuClassesMapping(): Map<LWClassifier<*>, KClass<*>> {
        return astClassesAndClassifiers.bsToAsMap
    }

    fun getEnumerationsToKolasuClassesMapping(): Map<Enumeration, EnumKClass> {
        return classesAndEnumerations.bsToAsMap
    }

    fun getPrimitiveTypesToKolasuClassesMapping(): Map<LWPrimitiveType, KClass<*>> {
        return classesAndPrimitiveTypes.bsToAsMap
    }

    fun getKolasuClassesToEnumerationsMapping(): Map<EnumKClass, Enumeration> {
        return classesAndEnumerations.asToBsMap
    }

    fun getKolasuClassesToPrimitiveTypesMapping(): Map<KClass<*>, LWPrimitiveType> {
        return classesAndPrimitiveTypes.asToBsMap
    }

    @Deprecated("Use corresponding method using ConceptInterface")
    fun correspondingInterface(kClass: KClass<*>): Interface {
        return toLWClassifier(kClass) as Interface
    }

    fun correspondingClassifier(kClassifier: KClassifier): LWClassifier<*> {
        return when (kClassifier) {
            is ConceptInterface -> correspondingInterface(kClassifier)
            is KConcept -> correspondingLWConcept(kClassifier)
            is Annotation -> TODO()
        }
    }

    fun correspondingInterface(kConceptInterface: KConceptInterface): Interface {
        return (
            kConceptsAndLWConcepts.byA(kConceptInterface)
                ?: throw IllegalArgumentException("No equivalent found for $kConceptInterface")
        ) as Interface
    }

    fun correspondingLWConcept(kConcept: KConcept): Concept {
        return (
            kConceptsAndLWConcepts.byA(kConcept)
                ?: throw IllegalArgumentException("No equivalent found for $kConcept")
        ) as Concept
    }

    fun correspondingConcept(kClass: KClass<*>): Concept {
        return toLWClassifier(kClass) as Concept
    }

    fun correspondingConcept(nodeType: String): Concept {
        return toLWClassifier(nodeType) as Concept
    }

    fun correspondingKolasuClass(classifier: LWClassifier<*>): KClass<*>? {
        val partialResult =
            this
                .kConceptsAndLWConcepts
                .bsToAsMap
                .entries
                .find {
                    it.key.key == classifier.key &&
                        it.key.language!!.id == classifier.language!!.id &&
                        it.key.language!!.version == classifier.language!!.version
                }?.value
                ?.kClass()
        if (partialResult != null) {
            return partialResult
        }
        return this
            .astClassesAndClassifiers
            .bsToAsMap
            .entries
            .find {
                it.key.key == classifier.key &&
                    it.key.language!!.id == classifier.language!!.id &&
                    it.key.language!!.version == classifier.language!!.version
            }?.value
    }

    private fun registerMapping(
        kolasuClass: KClass<*>,
        featuresContainer: LWClassifier<*>,
    ) {
        astClassesAndClassifiers.associate(kolasuClass, featuresContainer)
    }

    private fun registerMapping(
        kConcept: com.strumenta.kolasu.language.Classifier,
        lwConcept: LWClassifier<*>,
    ) {
        kConceptsAndLWConcepts.associate(kConcept, lwConcept)
    }

    private fun toLWClassifier(kClass: KClass<*>): LWClassifier<*> {
        return astClassesAndClassifiers.byA(kClass) ?: throw IllegalArgumentException("Unknown KClass $kClass")
    }

    private fun toLWClassifier(nodeType: String): LWClassifier<*> {
        val kClassifier: LWClassifier<*>? =
            kConceptsAndLWConcepts
                .asToBsMap
                .entries
                .find {
                    it.key.name == nodeType
                }?.value
        if (kClassifier != null) {
            return kClassifier
        }
        val kClass =
            astClassesAndClassifiers.`as`.find { it.qualifiedName == nodeType || it.simpleName == nodeType }
                ?: throw IllegalArgumentException(
                    "Unknown nodeType $nodeType",
                )
        return toLWClassifier(kClass)
    }

    @Deprecated("Use equivalent for EnumType")
    private fun toLWEnumeration(
        kClass: KClass<*>,
        lionwebLanguage: LWLanguage,
    ): Enumeration {
        val enumeration = classesAndEnumerations.byA(kClass as EnumKClass)
        if (enumeration == null) {
            val newEnumeration = addEnumerationFromClass(lionwebLanguage, kClass)
            classesAndEnumerations.associate(kClass, newEnumeration)
            return newEnumeration
        } else {
            return enumeration
        }
    }

    private fun toLWEnumeration(
        kEnumType: EnumType,
        lionwebLanguage: LWLanguage,
    ): Enumeration {
        val enumeration = kEnumerationAndLWEnumerations.byA(kEnumType)
        if (enumeration == null) {
            val newEnumeration = Enumeration(lionwebLanguage, kEnumType.name)
            newEnumeration.id = (lionwebLanguage.id ?: "unknown_language") + "_" + newEnumeration.name
            newEnumeration.key = newEnumeration.name

            kEnumType.literals.forEach { entry ->
                newEnumeration.addLiteral(
                    EnumerationLiteral(newEnumeration, entry.name).apply {
                        id = newEnumeration.id + "-" + entry.name
                        key = newEnumeration.key + "-" + entry.name
                    },
                )
            }

            lionwebLanguage.addElement(newEnumeration)
            kEnumerationAndLWEnumerations.associate(kEnumType, newEnumeration)
            return newEnumeration
        } else {
            return enumeration
        }
    }

    @Deprecated("Use equivalent for PrimitiveType")
    private fun toLWPrimitiveType(
        kClass: KClass<*>,
        lionwebLanguage: LWLanguage,
    ): LWPrimitiveType {
        val primitiveType = classesAndPrimitiveTypes.byA(kClass)
        if (primitiveType == null) {
            val newPrimitiveName = kClass.simpleName
            val newPrimitiveTypeID = (lionwebLanguage.id ?: "unknown_language") + "_" + newPrimitiveName
            val newPrimitiveType = LWPrimitiveType(lionwebLanguage, newPrimitiveName, newPrimitiveTypeID)
            newPrimitiveType.setKey(newPrimitiveName)
            lionwebLanguage.addElement(newPrimitiveType)
            classesAndPrimitiveTypes.associate(kClass, newPrimitiveType)
            return newPrimitiveType
        } else {
            return primitiveType
        }
    }

    private fun toLWPrimitiveType(
        kPrimitiveType: KPrimitiveType,
        lionwebLanguage: LWLanguage,
    ): LWPrimitiveType {
        val lwPrimitiveType = primitiveTypesMapping.byA(kPrimitiveType)
        if (lwPrimitiveType == null) {
            val newPrimitiveName = kPrimitiveType.name
            val newPrimitiveTypeID = (lionwebLanguage.id ?: "unknown_language") + "_" + newPrimitiveName
            val newPrimitiveType = LWPrimitiveType(lionwebLanguage, newPrimitiveName, newPrimitiveTypeID)
            newPrimitiveType.setKey(newPrimitiveName)
            lionwebLanguage.addElement(newPrimitiveType)
            primitiveTypesMapping.associate(kPrimitiveType, newPrimitiveType)
            return newPrimitiveType
        } else {
            return lwPrimitiveType
        }
    }

    private fun toLWDataType(
        kDataType: KDataType,
        lionwebLanguage: LWLanguage,
    ): DataType<*> {
        return when {
            intType == kDataType -> LionCoreBuiltins.getInteger()
            stringType == kDataType -> LionCoreBuiltins.getString()
            kDataType is EnumType -> {
                val enumeration = kEnumerationAndLWEnumerations.byA(kDataType)
                if (enumeration == null) {
                    val newEnumeration = Enumeration(lionwebLanguage, kDataType.name)
                    newEnumeration.id = (lionwebLanguage.id ?: "unknown_language") + "_" + newEnumeration.name
                    newEnumeration.key = newEnumeration.name
                    lionwebLanguage.addElement(newEnumeration)
                    kEnumerationAndLWEnumerations.associate(kDataType, newEnumeration)
                    // TODO add literals
                    newEnumeration
                } else {
                    enumeration
                }
            }
            else -> TODO(kDataType.toString())
        }
    }

    private fun toLWDataType(
        kType: KType,
        lionwebLanguage: LWLanguage,
    ): DataType<*> {
        return when (kType) {
            Int::class.createType() -> LionCoreBuiltins.getInteger()
            Long::class.createType() -> LionCoreBuiltins.getInteger()
            String::class.createType() -> LionCoreBuiltins.getString()
            Boolean::class.createType() -> LionCoreBuiltins.getBoolean()
            Char::class.createType() -> StarLasuLWLanguage.char
            else -> {
                val kClass = kType.classifier as KClass<*>
                val isEnum = kClass.supertypes.any { it.classifier == Enum::class }
                if (isEnum) {
                    return toLWEnumeration(kClass, lionwebLanguage)
                } else {
                    return toLWPrimitiveType(kClass, lionwebLanguage)
                }
            }
        }
    }
}

fun com.strumenta.kolasu.language.Classifier.kClass(): KClass<*> =
    if (this is com.strumenta.kolasu.language.Concept) {
        this.correspondingKotlinClass
    } else {
        null
    } ?: Class.forName(this.qualifiedName).kotlin

fun com.strumenta.kolasu.language.DataType.kClass(): KClass<*> =
    when {
        this == intType -> Int::class
        this == stringType -> String::class
        this == booleanType -> Boolean::class
        else -> Class.forName(this.name).kotlin
    }

fun addEnumerationFromClass(
    lionwebLanguage: LWLanguage,
    kClass: EnumKClass
): Enumeration {
    val newEnumeration = Enumeration(lionwebLanguage, kClass.simpleName)
    newEnumeration.id = (lionwebLanguage.id ?: "unknown_language") + "_" + newEnumeration.name
    newEnumeration.key = newEnumeration.name

    val entries = kClass.java.enumConstants
    entries.forEach { entry ->
        newEnumeration.addLiteral(
            EnumerationLiteral(newEnumeration, entry.name).apply {
                id = newEnumeration.id + "-" + entry.name
                key = newEnumeration.key + "-" + entry.name
            }
        )
    }

    lionwebLanguage.addElement(newEnumeration)
    return newEnumeration
}
