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
                        prop.key = featuresContainer.key + "_" + prop.name
                        prop.id = featuresContainer.id + "_" + prop.name
                        prop.setOptional(it.optional)
                        prop.setType(toLWDataType(it.type))
                        featuresContainer.addFeature(prop)
                    }
                    is Reference -> {
                        val ref = io.lionweb.lioncore.java.language.Reference(it.name, featuresContainer)
                        ref.key = featuresContainer.key + "_" + ref.name
                        ref.id = featuresContainer.id + "_" + ref.name
                        ref.setOptional(it.optional)
                        ref.setType(toLWFeaturesContainer(it.type))
                        featuresContainer.addFeature(ref)
                    }
                    is Containment -> {
                        val cont = io.lionweb.lioncore.java.language.Containment(it.name, featuresContainer)
                        cont.key = featuresContainer.key + "_" + cont.name
                        cont.id = featuresContainer.id + "_" + cont.name
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
            Long::class.createType() -> LionCoreBuiltins.getInteger()
            String::class.createType() -> LionCoreBuiltins.getString()
            Boolean::class.createType() -> LionCoreBuiltins.getBoolean()
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

    fun matchingKClass(concept: Concept): KClass<*>? {
        return this.LWConceptToKolasuClass.entries.find {
            it.key.key == concept.key &&
                it.key.language!!.id == concept.language!!.id &&
                it.key.language!!.version == concept.language!!.version
        }?.value
    }
//
//    private fun instantiate(
//        constructor: KFunction<out Node>,
//        concept: Concept,
//        serializedNode: SerializedNode,
//        jsonSerialization: JsonSerialization,
//        unserializedNodesByID: Map<String, Node>,
//        propertiesValues: Map<Property, Any?>
//    ): Node {
//        val parameters = mutableMapOf<KParameter, Any?>()
//        constructor.parameters.forEach { constructorParameter ->
//            // Is this constructor parameter corresponding to some feature?
//            val feature = concept.allFeatures().find { f -> f.name == constructorParameter.name }
//            if (feature == null) {
//                TODO()
//            } else {
//                when (feature) {
//                    is Property -> {
//                        // We can then unserialize the feature and pass that value
//                        val serializedPropertyValue = serializedNode.properties.find {
//                            it.metaPointer.key == feature.key
//                        }
//                        if (serializedPropertyValue == null) {
//                            throw IllegalStateException("Missing value for property ${feature.name}")
//                        } else {
//                            parameters[constructorParameter] = propertiesValues[feature as Property]
//                        }
//                    }
//
//                    is LWContainment -> {
//                        val serializedContainmentValue = serializedNode.containments.find {
//                            it.metaPointer.key == feature.key
//                        }
//                        if (serializedContainmentValue == null) {
//                            throw IllegalStateException("Missing value for containment ${feature.name}")
//                        } else {
//                            val containment = feature
//                            val childrenIDs = serializedContainmentValue.value as List<String>
//                            val children: MutableList<out Node> = childrenIDs.map { childID ->
//                                unserializedNodesByID[childID] ?: throw IllegalStateException()
//                            }.toMutableList()
//                            if (!containment.isMultiple) {
//                                when (children.size) {
//                                    0 -> parameters[constructorParameter] = null
//                                    1 -> parameters[constructorParameter] = children[0]
//                                    else -> throw IllegalStateException()
//                                }
//                            } else {
//                                parameters[constructorParameter] = children
//                            }
//                        }
//                    }
//
//                    is LWReference -> {
//                        TODO("References in constructor not yet supported")
//                    }
//                    else -> throw IllegalStateException()
//                }
//            }
//        }
//        try {
//            val node = constructor.callBy(parameters)
//            node.assignParents()
//            return node
//        } catch (t: Throwable) {
//            throw RuntimeException("Invocation of constructor $constructor failed. Parameters: $parameters", t)
//        }
//    }
//
//    fun prepareSerialization(jsonSerialization: JsonSerialization, kolasuLanguage: KolasuLanguage) {
//        val lionwebLanguage = correspondingLanguage(kolasuLanguage)
//        jsonSerialization.conceptResolver.registerLanguage(lionwebLanguage)
//        this.LWConceptToKolasuClass.filter { !it.value.isAbstract }.forEach {
//            val concept = it.key
//            val kolasuClass = it.value
//            jsonSerialization.nodeInstantiator.registerCustomUnserializer(concept.id) { concept, serializedNode,
//                                                                                        unserializedNodesByID,
//                                                                                        propertiesValue ->
//                val primaryConstructor = kolasuClass.primaryConstructor
//                if (primaryConstructor == null) {
//                    val emptyLikeConstructor = kolasuClass.constructors.any { it.parameters.all { it.isOptional } }
//                    if (emptyLikeConstructor == null) {
//                        val firstConstructor = kolasuClass.constructors.first()
//                        if (firstConstructor == null) {
//                            TODO()
//                        } else {
//                            instantiate(
//                                firstConstructor,
//                                concept!!,
//                                serializedNode!!,
//                                jsonSerialization,
//                                unserializedNodesByID,
//                                propertiesValue
//                            )
//                        }
//                    } else {
//                        TODO()
//                    }
//                } else {
//                    instantiate(
//                        primaryConstructor,
//                        concept!!,
//                        serializedNode!!,
//                        jsonSerialization,
//                        unserializedNodesByID,
//                        propertiesValue
//                    )
//                }
//            }
//        }
//        mappedEnumerations.forEach { entry ->
//            val enumeration = entry.value
//            jsonSerialization.primitiveValuesSerialization.registerSerializer(
//                enumeration.id,
//                PrimitiveValuesSerialization.PrimitiveSerializer<Enum<*>> { enum -> enum.name }
//            )
//        }
//    }
}
