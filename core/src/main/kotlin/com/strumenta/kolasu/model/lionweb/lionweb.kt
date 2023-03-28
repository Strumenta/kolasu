package com.strumenta.kolasu.model.lionweb

import com.strumenta.kolasu.metamodel.StarLasuMetamodel
import com.strumenta.kolasu.model.*
import org.lionweb.lioncore.java.metamodel.*
import org.lionweb.lioncore.java.metamodel.Metamodel
import org.lionweb.lioncore.java.serialization.JsonSerialization
import java.lang.IllegalStateException
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KProperty1
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.allSupertypes


open class ReflectionBasedMetamodel(id: String, name: String, version: Int, vararg val classes: KClass<*>) : Metamodel() {
    private val mappedConcepts = mutableMapOf<KClass<out ASTNode>, Concept>()
    private val mappedConceptInterfaces = mutableMapOf<KClass<*>, ConceptInterface>()
    private val mappedEnums = mutableMapOf<KClass<out Enum<*>>, Enum<*>>()
    companion object {
        // This is used as replacement for the object singleton, as it is not assigned
        // before init has terminated
        val INSTANCES = HashMap<KClass<*>, ReflectionBasedMetamodel>()
    }

    constructor(vararg classes: KClass<*>) : this("TEMP", "TEMP", *classes) {
        setID(this.javaClass.canonicalName)
        setKey(this.javaClass.canonicalName)
        setName(this.javaClass.canonicalName)
    }

    init {
        setID(id)
        setKey(id)
        setName(name)
        setVersion(Integer.toString(version))
        INSTANCES[this.javaClass.kotlin] = this
        classes.forEach { considerClass(it) }
    }

    constructor(id: String, name: String, vararg classes: KClass<*>) : this(id, name, 1, *classes)

    fun prepareSerialization(jsonSerialization: JsonSerialization) {
//        enumClasses.forEach { enumClass ->
//            val enumDeclaration = enumClass.enumeration
//            jsonSerialization.primitiveValuesSerialization.registerSerializer(enumDeclaration.id,
//                PrimitiveSerializer<Enum<*>> { it.name })
//        }
        TODO()
    }

    private fun processProperty(featuresContainer: FeaturesContainer<*>, kotlinProperty: KProperty1<in ASTNode, *>) {
        val property = Property()
        property.simpleName = kotlinProperty.name
        property.id = kotlinProperty.name
        property.key = "${featuresContainer.key}-${kotlinProperty.name}"
        when (kotlinProperty.returnType.classifier) {
            Boolean::class -> {
                property.type = LionCoreBuiltins.getBoolean()
            }

            String::class -> {
                property.type = LionCoreBuiltins.getString()
            }

            Int::class -> {
                property.type = LionCoreBuiltins.getInteger()
            }

            Position::class -> {
                property.type = StarLasuMetamodel.position
            }

            Char::class -> {
                property.type = StarLasuMetamodel.char
            }

            else -> {
                if ((kotlinProperty.returnType.classifier as? KClass<*>)?.allSuperclasses?.contains(Enum::class) == true
                ) {
                    val enum : KClass<out Enum<*>> = kotlinProperty.returnType.classifier as KClass<out Enum<*>>
                    considerClass(enum)
                    property.type = enum.enumeration
                } else {
                    TODO(
                        "Return type: ${kotlinProperty.returnType.classifier} " +
                            "(${kotlinProperty.returnType.classifier?.javaClass} for property $kotlinProperty"
                    )
                }
            }
        }
        concept.addFeature(property)
    }

    private fun processContainment(featuresContainer: FeaturesContainer<*>, kotlinProperty: KProperty1<in ASTNode, *>) {
        val containment = Containment()
        containment.simpleName = kotlinProperty.name
        containment.id = kotlinProperty.name
          containment.key = "${featuresContainer.key}-${kotlinProperty.name}"
        if ((kotlinProperty.returnType.classifier as KClass<*>).allSupertypes.map { it.classifier }
                .contains(Collection::class)
        ) {
            considerClass(kotlinProperty.returnType.arguments[0].type!!
                .classifier as KClass<*>)
            containment.isMultiple = true
            containment.type = kotlinProperty.returnType.arguments[0].type!!
                    .classifier!!.asFeaturesContainer()
        } else {
            val classifier = kotlinProperty.returnType.classifier
            considerClass(classifier as KClass<*>)
            containment.type = classifier!!.asFeaturesContainer()
        }

        featuresContainer.addFeature(containment)
    }

    private fun processConcept(kClass: KClass<out ASTNode>) {
        val concept = Concept()
        concept.simpleName = kClass.simpleName
        concept.id = this.key + "-" + kClass.simpleName
        concept.key = this.key + "-" + kClass.simpleName

        addElement(concept)
        mappedConcepts[kClass] = concept


        kClass.nodeProperties.forEach { kotlinProperty ->
            val provideNodes = PropertyDescription.providesNodes(kotlinProperty as KProperty1<in ASTNode, *>)
            val isReference = (kotlinProperty as KProperty1<in ASTNode, *>).isReference
            when {
                !provideNodes -> {
                    if (isReference) {
//                        considerClass(kotlinProperty.returnType.arguments[0].type!!
//                            .classifier as KClass<*>)
                        TODO()
                    } else {
                        processProperty(concept, kotlinProperty)
                    }
                }
                provideNodes && isReference -> TODO()
                provideNodes && !isReference -> {
                    processContainment(concept, kotlinProperty)
                }
            }
        }
//            kClass.superclasses.forEach { considerClass(it) }
//            kClass.sealedSubclasses.forEach { considerClass(it) }
//            if (kClass.java.isInterface) {
//                kClass.conceptInterface
//            } else {
//                (kClass as? KClass<out ASTNode>)?.concept
//            }
    }

    private fun considerClass(kClass: KClass<*>) {
        try {
            if (mappedConcepts.containsKey(kClass)
                || mappedConceptInterfaces.containsKey(kClass)
                || mappedEnums.containsKey(kClass)
                || kClass == ASTNode::class || kClass == Any::class) {
                return
            }
            when {
                kClass.isEnum -> {
                    // Process the enum
                    // add to mapped enums
                    TODO()
                }
                kClass.isInterface -> {
                    TODO()
                }
                kClass.isASTNode -> {
                    if (this == requireMetamodelFor(kClass)) {
                        processConcept(kClass as KClass<out ASTNode>)
                    } else {
                        TODO()
                    }
                }
                else -> {
                    // The class is irrelevant for us
                    return
                }
            }
//            consideredClasses.add(kClass)
//            kClass.nodeProperties.forEach {
//                val provideNodes = PropertyDescription.providesNodes(it as KProperty1<in ASTNode, *>)
//                val ref = (it as KProperty1<in ASTNode, *>).isReference
//                when {
//                    !provideNodes -> {
//                        if (it.returnType.classifier == ReferenceByName::class) {
//                            considerClass(it.returnType.arguments[0].type!!
//                                .classifier as KClass<*>)
//                        } else {
//                            when (it.returnType.classifier) {
//                                Boolean::class, String::class,Int::class,Position::class,Char::class -> Unit // nothing to do
//                                else -> {
//                                    if ((it.returnType.classifier as? KClass<*>)?.allSuperclasses?.contains(Enum::class)
//                                            ?: false
//                                    ) {
//                                        val enum : KClass<out Enum<*>> = it.returnType.classifier as KClass<out Enum<*>>
//                                        considerClass(enum)
//                                    } else {
//                                        if ((it.returnType.classifier as? KClass<*>)?.allSuperclasses?.contains(java.util.List::class)
//                                                ?: false) {
//                                            throw RuntimeException("Illegal property of lists at $it")
//                                        } else {
//                                            TODO(
//                                                "Return type: ${it.returnType.classifier} " +
//                                                        "(${it.returnType.classifier?.javaClass} for property $it"
//                                            )
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                    provideNodes && ref -> TODO()
//                    provideNodes && !ref -> {
//                        val containment = Containment()
//                        containment.simpleName = it.name
//                        containment.id = it.name
//                        if ((it.returnType.classifier as KClass<*>).allSupertypes.map { it.classifier }
//                                .contains(Collection::class)
//                        ) {
//                            considerClass(it.returnType.arguments[0].type!!
//                                .classifier as KClass<*>)
//                        } else {
//                            val classifier = it.returnType.classifier
//                            considerClass(classifier as KClass<*>)
//                        }
//                    }
//                }
//            }
//            kClass.superclasses.forEach { considerClass(it) }
//            kClass.sealedSubclasses.forEach { considerClass(it) }
//            if (kClass.java.isInterface) {
//                kClass.conceptInterface
//            } else {
//                (kClass as? KClass<out ASTNode>)?.concept
//            }
        } catch (e: Throwable){
            System.err.println("Issue considering $kClass")
            e.printStackTrace()
            throw java.lang.RuntimeException("Issue considering $kClass", e)
        }
    }

    fun requireConceptFor(kClass: KClass<*>): Concept {
        return mappedConcepts[kClass]
            ?: throw IllegalStateException("No concept mapped to KClass $kClass in Metamodel $this")
    }
}



private fun <E : Enum<*>>calculateEnum(enum: KClass<E>) : Enumeration {
    val enumeration = Enumeration()
    enumeration.simpleName = enum.simpleName

    val metamodelInstance: Metamodel = metamodelFor(enum)
        ?: throw RuntimeException("No Metamodel object for $enum")

    enumeration.id = metamodelInstance.key + "-" + enum.simpleName
    enumeration.key = metamodelInstance.key + "-" + enum.simpleName

    metamodelInstance.addElement(enumeration)

    enum.java.enumConstants.forEach {
        val literal = EnumerationLiteral()
        literal.id = enum.simpleName + "-" + it.name
        literal.simpleName = enum.simpleName
        enumeration.addLiteral(literal)
    }

    return enumeration
}



fun <A : Any> calculateConceptInterface(
    kClass: KClass<A>,
    conceptsMemory: HashMap<KClass<out ASTNode>, Concept>,
    conceptInterfacesMemory: HashMap<KClass<out Any>, ConceptInterface>
): ConceptInterface {

    require(kClass.java.isInterface)

    val conceptInterface = ConceptInterface()
    conceptInterface.simpleName = kClass.simpleName

    val metamodelInstance: Metamodel = metamodelFor(kClass)
        ?: throw RuntimeException("No Metamodel object for $kClass")

    conceptInterface.id = metamodelInstance.key + "-" + kClass.simpleName
    conceptInterface.key = metamodelInstance.key + "-" + kClass.simpleName

    metamodelInstance.addElement(conceptInterface)

    // We need to add it right away because of nodes referring to themselves
    conceptInterfacesMemory[kClass] = conceptInterface
    return conceptInterface
}

private fun KClassifier.asFeaturesContainer(): FeaturesContainer<*> {
    return if ((this as KClass<*>).java.isInterface) {
        (this as KClass<out Any>).conceptInterface
    } else {
        (this as KClass<out ASTNode>).concept
    }
}

fun <A : ASTNode> calculateConcept(
    kClass: KClass<A>,
    conceptsMemory: HashMap<KClass<out ASTNode>, Concept>,
    conceptInterfacesMemory: HashMap<KClass<out Any>, ConceptInterface>
): Concept {
//    if (conceptsMemory.containsKey(kClass)) {
//        return conceptsMemory[kClass]!!
//    }
//    try {
//        require(!kClass.java.isInterface)
//        if (kClass == ASTNode::class) {
//            return StarLasuMetamodel.astNode
//        }
//        require(
//            kClass.allSuperclasses.contains(ASTNode::class) || kClass.findAnnotation<NodeType>() != null ||
//                kClass.allSuperclasses.any { it.findAnnotation<NodeType>() != null }
//        ) {
//            "KClass $kClass is not a subclass of ASTNode"
//        }
//        val concept = Concept()
//        concept.simpleName = kClass.simpleName
//
//        val superclasses = kClass.superclasses.toMutableList()
//        // require(superclasses.contains(ASTNode::class))
//        superclasses.remove(ASTNode::class)
//        superclasses.forEach { superclass ->
//            if (superclass.java.isInterface) {
//                // TODO consider
//            } else {
//                concept.extendedConcept = (superclass as KClass<out ASTNode>).concept
//            }
//        }
//        if (concept.extendedConcept == null) {
//            concept.extendedConcept = StarLasuMetamodel.astNode
//        }
//
//        val metamodelInstance: Metamodel = metamodelFor(kClass)
//            ?: throw RuntimeException("No Metamodel object for $kClass")
//        if (conceptsMemory.containsKey(kClass)) {
//            throw IllegalStateException("This should not happen")
//        }
//        metamodelInstance.addElement(concept)
//
//        concept.id = metamodelInstance.key + "-" + kClass.simpleName
//        concept.key = metamodelInstance.key + "-" + kClass.simpleName
//
//        // We need to add it right away because of nodes referring to themselves
//        conceptsMemory[kClass] = concept
//
//        kClass.nodeProperties.forEach {
//            val provideNodes = PropertyDescription.providesNodes(it as KProperty1<in ASTNode, *>)
//            val ref = (it as KProperty1<in ASTNode, *>).isReference
//            when {
//                !provideNodes -> {
//                    if (it.returnType.classifier == ReferenceByName::class) {
//                        val reference = Reference()
//                        reference.simpleName = it.name
//                        reference.id = it.name
//                        reference.key = "${concept.key}-${it.name}"
//                        reference.type = it.returnType.arguments[0].type!!
//                                .classifier!!.asFeaturesContainer()
//                        concept.addFeature(reference)
//                    } else {
//                        val property = Property()
//                        property.simpleName = it.name
//                        property.id = it.name
//                        property.key = "${concept.key}-${it.name}"
//                        when (it.returnType.classifier) {
//                            Boolean::class -> {
//                                property.type = LionCoreBuiltins.getBoolean()
//                            }
//
//                            String::class -> {
//                                property.type = LionCoreBuiltins.getString()
//                            }
//
//                            Int::class -> {
//                                property.type = LionCoreBuiltins.getInteger()
//                            }
//
//                            Position::class -> {
//                                property.type = StarLasuMetamodel.position
//                            }
//
//                            Char::class -> {
//                                property.type = StarLasuMetamodel.char
//                            }
//
//                            else -> {
//                                if ((it.returnType.classifier as? KClass<*>)?.allSuperclasses?.contains(Enum::class)
//                                    ?: false
//                                ) {
//                                    val enum : KClass<out Enum<*>> = it.returnType.classifier as KClass<out Enum<*>>
//                                    property.type = enum.enumeration
//                                } else {
//                                    TODO(
//                                        "Return type: ${it.returnType.classifier} " +
//                                            "(${it.returnType.classifier?.javaClass} for property $it"
//                                    )
//                                }
//                            }
//                        }
//                        concept.addFeature(property)
//                    }
//                }
//                provideNodes && ref -> TODO()
//                provideNodes && !ref -> {
//                    val containment = Containment()
//                    containment.simpleName = it.name
//                    containment.id = it.name
//                    containment.key = "${concept.key}-${it.name}"
//                    if ((it.returnType.classifier as KClass<*>).allSupertypes.map { it.classifier }
//                        .contains(Collection::class)
//                    ) {
//                        containment.isMultiple = true
//                        containment.type = it.returnType.arguments[0].type!!
//                                .classifier!!.asFeaturesContainer()
//                    } else {
//                        val classifier = it.returnType.classifier
//                        containment.type = classifier!!.asFeaturesContainer()
//                    }
//                    concept.addFeature(containment)
//                }
//            }
//        }
//        return concept
    TODO()
//    } catch (e: Throwable) {
//        throw RuntimeException("Unable to calculate concept for $kClass", e)
//    }
}
