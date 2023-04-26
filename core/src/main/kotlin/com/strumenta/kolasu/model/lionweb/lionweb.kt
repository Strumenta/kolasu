package com.strumenta.kolasu.model.lionweb

import com.strumenta.kolasu.metamodel.StarLasuMetamodel
import com.strumenta.kolasu.model.*
import org.lionweb.lioncore.java.metamodel.*
import org.lionweb.lioncore.java.metamodel.Metamodel
import org.lionweb.lioncore.java.serialization.JsonSerialization
import org.lionweb.lioncore.java.serialization.PrimitiveValuesSerialization
import java.lang.IllegalStateException
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KProperty1
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.superclasses

open class ReflectionBasedMetamodel(id: String, name: String, version: Int, vararg val classes: KClass<*>) :
    Metamodel() {
    private val mappedConcepts = mutableMapOf<KClass<out ASTNode>, Concept>()
    private val mappedConceptInterfaces = mutableMapOf<KClass<*>, ConceptInterface>()
    private val mappedEnumerations = mutableMapOf<KClass<out Enum<*>>, Enumeration>()
    private val populated = mutableSetOf<KClass<*>>()
    companion object {
        // This is used as replacement for the object singleton, as it is not assigned
        // before init has terminated
        val INSTANCES = HashMap<KClass<*>, ReflectionBasedMetamodel>()
    }

    constructor(id: String, name: String, vararg classes: KClass<*>) : this(id, name, 1, *classes)

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
        instantiateModelElements()
        populateModelElements()
        mappedConcepts.forEach { recordConceptForClass(it.key.java, it.value) }
        mappedConceptInterfaces.forEach { recordConceptInterfaceForClass(it.key.java, it.value) }
        mappedEnumerations.forEach { recordEnumerationForClass(it.key.java, it.value) }
    }

    // /
    // / Public methods
    // /

    fun requireConceptFor(kClass: KClass<*>): Concept {
        getRecordedConcept(kClass as KClass<out ASTNode>)?.let {
            return it
        }
        val mm = requireMetamodelFor(kClass)
        return if (this == mm) {
            mappedConcepts[kClass]
                ?: throw IllegalStateException("No Concept mapped to KClass $kClass in Metamodel $this")
        } else {
            throw IllegalStateException(
                "Not supporting external metamodels: " +
                    "referring to $mm while processing $this. Processing $kClass"
            )
        }
    }

    fun requireConceptInterfaceFor(kClass: KClass<*>): ConceptInterface {
        getRecordedConceptInterface(kClass)?.let { return it }
        val mm = requireMetamodelFor(kClass)
        return if (this == mm) {
            mappedConceptInterfaces[kClass]
                ?: throw IllegalStateException("No ConceptInterface mapped to KClass $kClass in Metamodel $this")
        } else {
            throw IllegalStateException(
                "Unable to find concept interface for $kClass, which is referring to external metamodel $mm. " +
                    "We are processing metamodel $this. Known classes: " +
                    "${getKnownClassesForConceptInterfaces().joinToString(", ") {
                        it.qualifiedName ?: it.toString()
                    }}"
            )
        }
    }

    fun requireEnumerationFor(kClass: KClass<*>): Enumeration {
        getRecordedEnum(kClass as KClass<out Enum<*>>)?.let { return it }
        val mm = requireMetamodelFor(kClass)
        return if (this == mm) {
            mappedEnumerations[kClass]
                ?: throw IllegalStateException("No Enumeration mapped to KClass $kClass in Metamodel $this")
        } else {
            throw IllegalStateException(
                "Unable to find enumeration for $kClass, which is referring to external metamodel $mm. " +
                    "We are processing metamodel $this. Known classes: " +
                    "${getKnownClassesForEnumerations().joinToString(", ") {
                        it.qualifiedName ?: it.toString()
                    }}"
            )
        }
    }

    fun prepareSerialization(jsonSerialization: JsonSerialization) {
        mappedEnumerations.forEach { entry ->
            val enumeration = entry.value
            jsonSerialization.primitiveValuesSerialization.registerSerializer(
                enumeration.id,
                PrimitiveValuesSerialization.PrimitiveSerializer<Enum<*>> { enum -> enum.name }
            )
        }
    }

    // /
    // / Instantiating
    // /

    private fun instantiateModelElements() {
        classes.forEach { scanAndInstantiate(it) }
    }

    private fun scanAndInstantiate(kClass: KClass<*>) {
        if (mappedConcepts.containsKey(kClass) ||
            mappedConceptInterfaces.containsKey(kClass) ||
            mappedEnumerations.containsKey(kClass) ||
            kClass == Any::class
        ) {
            return
        }
        if (isRecorded(kClass)) {
            return
        }
        when {
            kClass.isEnum -> {
                val mm = requireMetamodelFor(kClass)
                if (this == mm) {
                    scanAndInstantiateEnumeration(kClass as KClass<out Enum<*>>)
                }
            }

            kClass.isInterface -> {
                val mm = requireMetamodelFor(kClass)
                if (this == mm) {
                    scanAndInstantiateConceptInterface(kClass)
                }
            }

            kClass.isASTNode -> {
                val mm = requireMetamodelFor(kClass)
                if (this == mm) {
                    scanAndInstantiateConcept(kClass as KClass<out ASTNode>)
                }
            }

            else -> {
                // The class is irrelevant for us
                return
            }
        }
    }

    private fun scanAndInstantiateConcept(kClass: KClass<out ASTNode>) {
        val concept = Concept()
        scanAndInstantiateMetamodelElement(concept, kClass) {
            mappedConcepts[kClass] = concept
        }
    }

    private fun scanAndInstantiateConceptInterface(kClass: KClass<*>) {
        val conceptInterface = ConceptInterface()
        scanAndInstantiateMetamodelElement(conceptInterface, kClass) {
            mappedConceptInterfaces[kClass] = conceptInterface
        }
    }

    private fun scanAndInstantiateEnumeration(kClass: KClass<out Enum<*>>) {
        val enumeration = Enumeration()
        scanAndInstantiateMetamodelElement(enumeration, kClass) {
            mappedEnumerations[kClass] = enumeration
            recordEnumerationForClass(kClass.java, enumeration)
        }
    }

    private fun <ME : MetamodelElement<*>> scanAndInstantiateMetamodelElement(
        metamodelElement: ME,
        kClass: KClass<*>,
        mapper: (metamodelElement: ME) -> Unit
    ) {
        metamodelElement.name = kClass.simpleName
        metamodelElement.id = this.key + "-" + kClass.simpleName
        metamodelElement.key = this.key + "-" + kClass.simpleName

        addElement(metamodelElement)
        mapper(metamodelElement)

        kClass.nodeProperties.forEach(::scanKotlinProperty)
        kClass.superclasses.forEach { scanAndInstantiate(it) }
        kClass.sealedSubclasses.forEach { scanAndInstantiate(it) }
    }

    private fun scanKotlinProperty(kotlinProperty: KProperty1<*, *>) {
        val provideNodes = PropertyDescription.providesNodes(kotlinProperty as KProperty1<in ASTNode, *>)
        val isReference = kotlinProperty.isReference
        when {
            !provideNodes -> {
                if (isReference) {
                    val referenceTargetType = kotlinProperty.returnType.arguments[0].type!!.classifier as KClass<*>
                    scanAndInstantiate(referenceTargetType)
                } else {
                    if ((kotlinProperty.returnType.classifier as KClass<*>).allSupertypes.map { it.classifier }
                        .contains(Collection::class)
                    ) {
                        scanAndInstantiate(
                            kotlinProperty.returnType.arguments[0].type!!
                                .classifier as KClass<*>
                        )
                    } else {
                        val classifier = kotlinProperty.returnType.classifier
                        scanAndInstantiate(classifier as KClass<*>)
                    }
                }
            }
            provideNodes && isReference -> {
                throw IllegalStateException()
            }
            provideNodes && !isReference -> {
                if ((kotlinProperty.returnType.classifier as KClass<*>).allSupertypes.map { it.classifier }
                    .contains(Collection::class)
                ) {
                    scanAndInstantiate(
                        kotlinProperty.returnType.arguments[0].type!!
                            .classifier as KClass<*>
                    )
                } else {
                    val classifier = kotlinProperty.returnType.classifier
                    scanAndInstantiate(classifier as KClass<*>)
                }
            }
        }
    }

    // /
    // / Populating
    // /

    private fun populateModelElements() {
        // we need to process classes starting from the ancestors
        val classes = mappedConcepts.keys + mappedConceptInterfaces.keys + mappedEnumerations.keys
        classes.sortedBy { it.allSupertypes.size }.forEach { populate(it) }
    }

    private fun populate(kClass: KClass<*>) {
        if (populated.contains(kClass)) {
            return
        }
        populated.add(kClass)
        if (kClass.isEnum) {
            val enumeration = mappedEnumerations[kClass]!!
            kClass.java.enumConstants.forEach { enumConstant ->
                val literal = EnumerationLiteral()
                literal.id = kClass.simpleName + "-" + enumConstant.toString()
                literal.name = kClass.simpleName
                enumeration.addLiteral(literal)
            }
        } else if (kClass.isInterface) {
            val conceptInterface = mappedConceptInterfaces[kClass]!!
            kClass.superclasses.forEach {
                if (it.isInterface) {
                    conceptInterface.addExtendedInterface(requireConceptInterfaceFor(it))
                }
            }
            kClass.nodeProperties.forEach { kotlinProperty ->
                populateKotlinProperty(conceptInterface, kotlinProperty)
            }
        } else {
            val concept = mappedConcepts[kClass]!!
            kClass.superclasses.forEach {
                if (it.isInterface) {
                    concept.addImplementedInterface(requireConceptInterfaceFor(it))
                } else {
                    concept.extendedConcept = requireConceptFor(it)
                }
            }
            kClass.nodeProperties.forEach { kotlinProperty ->
                populateKotlinProperty(concept, kotlinProperty)
            }
        }
    }

    private fun populateKotlinProperty(featuresContainer: FeaturesContainer<*>, kotlinProperty: KProperty1<*, *>) {
        if (featuresContainer.allFeatures().any { it.name == kotlinProperty.name }) {
            // we are overriding an existing property, ignoring
            return
        }
        val provideNodes = PropertyDescription.providesNodes(kotlinProperty as KProperty1<in ASTNode, *>)
        val isReference = kotlinProperty.isReference
        when {
            !provideNodes -> {
                if (isReference) {
                    populateReference(featuresContainer, kotlinProperty)
                } else {
                    populateProperty(featuresContainer, kotlinProperty)
                }
            }

            provideNodes && isReference -> throw IllegalStateException()
            provideNodes && !isReference -> {
                populateContainment(featuresContainer, kotlinProperty)
            }
        }
    }

    private fun populateProperty(featuresContainer: FeaturesContainer<*>, kotlinProperty: KProperty1<in ASTNode, *>) {
        val property = Property()
        property.name = kotlinProperty.name
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
                    val enum: KClass<out Enum<*>> = kotlinProperty.returnType.classifier as KClass<out Enum<*>>
                    property.type = requireEnumerationFor(enum)
                } else {
                    TODO(
                        "Return type: ${kotlinProperty.returnType.classifier} " +
                            "(${kotlinProperty.returnType.classifier?.javaClass} for property $kotlinProperty"
                    )
                }
            }
        }
        featuresContainer.addFeature(property)
    }

    private fun populateContainment(
        featuresContainer: FeaturesContainer<*>,
        kotlinProperty: KProperty1<in ASTNode, *>
    ) {
        val containment = Containment()
        containment.name = kotlinProperty.name
        containment.id = kotlinProperty.name
        containment.key = "${featuresContainer.key}-${kotlinProperty.name}"
        if ((kotlinProperty.returnType.classifier as KClass<*>).allSupertypes.map { it.classifier }
            .contains(Collection::class)
        ) {
            containment.isMultiple = true
            containment.type = kotlinProperty.returnType.arguments[0].type!!
                .classifier!!.asFeaturesContainer()
        } else {
            val classifier = kotlinProperty.returnType.classifier
            containment.type = classifier!!.asFeaturesContainer()
        }

        featuresContainer.addFeature(containment)
    }

    private fun populateReference(featuresContainer: FeaturesContainer<*>, kotlinProperty: KProperty1<*, *>) {
        val reference = Reference()
        reference.name = kotlinProperty.name
        reference.id = kotlinProperty.name
        reference.key = "${featuresContainer.key}-${kotlinProperty.name}"
        val referenceTargetType = kotlinProperty.returnType.arguments[0].type!!
        val referenceTargetTypeClassifier = referenceTargetType.classifier as KClass<*>
        if (referenceTargetTypeClassifier.allSupertypes.map { it.classifier }
            .contains(Collection::class)
        ) {
            reference.isMultiple = true
            reference.type = referenceTargetType.arguments[0].type!!
                .classifier!!.asFeaturesContainer()
        } else {
            val classifier = referenceTargetTypeClassifier
            reference.type = classifier!!.asFeaturesContainer()
        }

        featuresContainer.addFeature(reference)
    }
}

private fun KClassifier.asFeaturesContainer(): FeaturesContainer<*> {
    return if ((this as KClass<*>).java.isInterface) {
        (this as KClass<out Any>).conceptInterface
    } else {
        (this as KClass<out ASTNode>).concept
    }
}
