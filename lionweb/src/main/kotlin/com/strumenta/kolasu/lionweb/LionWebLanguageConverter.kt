package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.language.Attribute
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.language.Reference
import com.strumenta.kolasu.model.BehaviorDeclaration
import com.strumenta.kolasu.model.CommonElement
import com.strumenta.kolasu.model.Documentation
import com.strumenta.kolasu.model.EntityDeclaration
import com.strumenta.kolasu.model.EntityGroupDeclaration
import com.strumenta.kolasu.model.Expression
import com.strumenta.kolasu.model.Multiplicity
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Parameter
import com.strumenta.kolasu.model.PlaceholderElement
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.Statement
import com.strumenta.kolasu.model.TypeAnnotation
import com.strumenta.kolasu.model.declaredFeatures
import com.strumenta.kolasu.model.isConcept
import com.strumenta.kolasu.model.isConceptInterface
import com.strumenta.kolasu.model.isMarkedAsNodeType
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import com.strumenta.kolasu.validation.IssueType
import com.strumenta.starlasu.base.ASTLanguage
import io.lionweb.lioncore.java.language.Classifier
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.DataType
import io.lionweb.lioncore.java.language.Enumeration
import io.lionweb.lioncore.java.language.EnumerationLiteral
import io.lionweb.lioncore.java.language.Interface
import io.lionweb.lioncore.java.language.LionCoreBuiltins
import io.lionweb.lioncore.java.language.PrimitiveType
import io.lionweb.lioncore.java.language.Property
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType

/**
 * This class is able to convert between Kolasu and LionWeb languages, tracking the mapping.
 */
class LionWebLanguageConverter {
    private val astClassesAndClassifiers = BiMap<KClass<*>, Classifier<*>>()
    private val classesAndEnumerations = BiMap<EnumKClass, Enumeration>()
    private val classesAndPrimitiveTypes = BiMap<KClass<*>, PrimitiveType>()
    private val languages = BiMap<KolasuLanguage, LWLanguage>()

    init {
        val starLasuKLanguage = KolasuLanguage(ASTLanguage.getLanguage().name)
        languages.associate(starLasuKLanguage, ASTLanguage.getLanguage())
        registerMapping(Node::class, ASTLanguage.getASTNode())
        registerMapping(Named::class, LionCoreBuiltins.getINamed(LIONWEB_VERSION_USED_BY_KOLASU))
        registerMapping(PossiblyNamed::class, LionCoreBuiltins.getINamed(LIONWEB_VERSION_USED_BY_KOLASU))
        registerMapping(CommonElement::class, ASTLanguage.getCommonElement())
        registerMapping(BehaviorDeclaration::class, ASTLanguage.getBehaviorDeclaration())
        registerMapping(Documentation::class, ASTLanguage.getDocumentation())
        registerMapping(EntityDeclaration::class, ASTLanguage.getEntityDeclaration())
        registerMapping(EntityGroupDeclaration::class, ASTLanguage.getEntityGroupDeclaration())
        registerMapping(Expression::class, ASTLanguage.getExpression())
        registerMapping(Parameter::class, ASTLanguage.getParameter())
        registerMapping(PlaceholderElement::class, ASTLanguage.getPlaceholderElement())
        registerMapping(Statement::class, ASTLanguage.getStatement())
        registerMapping(TypeAnnotation::class, ASTLanguage.getTypeAnnotation())

        registerMapping(Issue::class, ASTLanguage.getIssue())
        classesAndEnumerations.associate(
            IssueSeverity::class,
            (ASTLanguage.getIssue().getFeatureByName(Issue::severity.name) as Property).type as Enumeration
        )
        classesAndEnumerations.associate(
            IssueType::class,
            (ASTLanguage.getIssue().getFeatureByName(Issue::type.name) as Property).type as Enumeration
        )
        registerMapping(ParsingResult::class, ASTLanguage.getParsingResult())
    }

    fun exportToLionWeb(kolasuLanguage: KolasuLanguage): LWLanguage {
        val lionwebLanguage = LWLanguage(LIONWEB_VERSION_USED_BY_KOLASU)
        lionwebLanguage.version = "1"
        lionwebLanguage.name = kolasuLanguage.qualifiedName
        lionwebLanguage.key = kolasuLanguage.qualifiedName.replace('.', '-')
        lionwebLanguage.id = "starlasu_language_${kolasuLanguage.qualifiedName.replace('.', '-')}"
        lionwebLanguage.addDependency(ASTLanguage.getLanguage())

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
                val superInterfaces = astClass.supertypes.map { it.classifier as KClass<*> }
                    .filter { it.java.isInterface }
                superInterfaces.filter { it.isMarkedAsNodeType() }.forEach {
                    conceptInterface.addExtendedInterface(correspondingInterface(it))
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
                    concept.addImplementedInterface(correspondingInterface(it))
                }
            }
            val features = try {
                astClass.declaredFeatures()
            } catch (e: RuntimeException) {
                throw RuntimeException("Issue processing features for AST class ${astClass.qualifiedName}", e)
            }

            features.forEach {
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
        this.languages.associate(kolasuLanguage, lionwebLanguage)
        return lionwebLanguage
    }

    /**
     * Importing a LionWeb language as a Kolasu language requires the generation of classes, to be performed
     * separately. Once that is done we associate the Kolasu language defined by those classes to a certain
     * LionWeb language, so that we can import LionWeb models by instantiating the corresponding classes in the
     * Kolasu language.
     */
    fun associateLanguages(lwLanguage: LWLanguage, kolasuLanguage: KolasuLanguage) {
        this.languages.associate(kolasuLanguage, lwLanguage)
        kolasuLanguage.astClasses.forEach { astClass ->
            var classifier: Classifier<*>? = null
            val annotation = astClass.annotations.filterIsInstance<LionWebAssociation>().firstOrNull()
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
            val annotation = enumClass.annotations.filterIsInstance<LionWebAssociation>().firstOrNull()
            if (annotation != null) {
                enumeration = lwLanguage.elements.filterIsInstance<Enumeration>().find {
                    it.key == annotation.key
                }
            }
            if (enumeration != null) {
                classesAndEnumerations.associate(enumClass, enumeration)
            }
        }
        kolasuLanguage.primitiveClasses.forEach { primitiveClass ->
            var primitiveType: PrimitiveType? = null
            val annotation = primitiveClass.annotations.filterIsInstance<LionWebAssociation>().firstOrNull()
            if (annotation != null) {
                primitiveType = lwLanguage.elements.filterIsInstance<PrimitiveType>().find {
                    it.key == annotation.key
                }
            }
            if (primitiveType != null) {
                classesAndPrimitiveTypes.associate(primitiveClass, primitiveType)
            }
        }
    }

    fun knownLWLanguages(): Set<LWLanguage> {
        return languages.bs
    }

    fun knownKolasuLanguages(): Set<KolasuLanguage> {
        return languages.`as`
    }

    fun correspondingLanguage(kolasuLanguage: KolasuLanguage): LWLanguage {
        return languages.byA(kolasuLanguage)
            ?: throw java.lang.IllegalArgumentException("Unknown Kolasu Language $kolasuLanguage")
    }

    fun correspondingLanguage(lwLanguage: LWLanguage): KolasuLanguage {
        return languages.byB(lwLanguage)
            ?: throw java.lang.IllegalArgumentException("Unknown LionWeb Language $lwLanguage")
    }

    fun getKolasuClassesToClassifiersMapping(): Map<KClass<*>, Classifier<*>> {
        return astClassesAndClassifiers.asToBsMap
    }

    fun getClassifiersToKolasuClassesMapping(): Map<Classifier<*>, KClass<*>> {
        return astClassesAndClassifiers.bsToAsMap
    }

    fun getEnumerationsToKolasuClassesMapping(): Map<Enumeration, EnumKClass> {
        return classesAndEnumerations.bsToAsMap
    }

    fun getPrimitiveTypesToKolasuClassesMapping(): Map<PrimitiveType, KClass<*>> {
        return classesAndPrimitiveTypes.bsToAsMap
    }

    fun getKolasuClassesToEnumerationsMapping(): Map<EnumKClass, Enumeration> {
        return classesAndEnumerations.asToBsMap
    }

    fun getKolasuClassesToPrimitiveTypesMapping(): Map<KClass<*>, PrimitiveType> {
        return classesAndPrimitiveTypes.asToBsMap
    }

    fun correspondingInterface(kClass: KClass<*>): Interface {
        return toLWClassifier(kClass) as Interface
    }

    fun correspondingConcept(kClass: KClass<*>): Concept {
        return toLWClassifier(kClass) as Concept
    }

    fun correspondingConcept(nodeType: String): Concept {
        return toLWClassifier(nodeType) as Concept
    }

    fun correspondingKolasuClass(classifier: Classifier<*>): KClass<*>? {
        return this.astClassesAndClassifiers.bsToAsMap.entries.find {
            it.key.key == classifier.key &&
                it.key.language!!.id == classifier.language!!.id &&
                it.key.language!!.version == classifier.language!!.version
        }?.value
    }

    private fun registerMapping(kolasuClass: KClass<*>, featuresContainer: Classifier<*>) {
        astClassesAndClassifiers.associate(kolasuClass, featuresContainer)
    }

    private fun toLWClassifier(kClass: KClass<*>): Classifier<*> {
        return astClassesAndClassifiers.byA(kClass) ?: throw IllegalArgumentException("Unknown KClass $kClass")
    }

    private fun toLWClassifier(nodeType: String): Classifier<*> {
        val kClass = astClassesAndClassifiers.`as`.find { it.qualifiedName == nodeType }
            ?: throw IllegalArgumentException(
                "Unknown nodeType $nodeType"
            )
        return toLWClassifier(kClass)
    }

    private fun toLWEnumeration(kClass: KClass<*>, lionwebLanguage: LWLanguage): Enumeration {
        val enumeration = classesAndEnumerations.byA(kClass as EnumKClass)
        if (enumeration == null) {
            val newEnumeration = addEnumerationFromClass(lionwebLanguage, kClass)
            classesAndEnumerations.associate(kClass, newEnumeration)
            return newEnumeration
        } else {
            return enumeration
        }
    }

    private fun toLWPrimitiveType(kClass: KClass<*>, lionwebLanguage: LWLanguage): PrimitiveType {
        val primitiveType = classesAndPrimitiveTypes.byA(kClass)
        if (primitiveType == null) {
            val newPrimitiveName = kClass.simpleName
            val newPrimitiveTypeID = (lionwebLanguage.id ?: "unknown_language") + "-" + newPrimitiveName + "-id"
            val newPrimitiveType = PrimitiveType(lionwebLanguage, newPrimitiveName, newPrimitiveTypeID)
            val newPrimitiveTypeKey = (lionwebLanguage.id ?: "unknown_language") + "-" + newPrimitiveName + "-key"
            newPrimitiveType.setKey(newPrimitiveTypeKey)
            lionwebLanguage.addElement(newPrimitiveType)
            classesAndPrimitiveTypes.associate(kClass, newPrimitiveType)
            return newPrimitiveType
        } else {
            return primitiveType
        }
    }

    private fun toLWDataType(kType: KType, lionwebLanguage: LWLanguage): DataType<*> {
        return when (kType) {
            Int::class.createType() -> LionCoreBuiltins.getInteger(LIONWEB_VERSION_USED_BY_KOLASU)
            Long::class.createType() -> LionCoreBuiltins.getInteger(LIONWEB_VERSION_USED_BY_KOLASU)
            String::class.createType() -> LionCoreBuiltins.getString(LIONWEB_VERSION_USED_BY_KOLASU)
            Boolean::class.createType() -> LionCoreBuiltins.getBoolean(LIONWEB_VERSION_USED_BY_KOLASU)
            Char::class.createType() -> ASTLanguage.getChar()
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
