package com.strumenta.kolasu.model

import com.strumenta.kolasu.metamodel.StarLasuMetamodel
import org.lionweb.lioncore.java.metamodel.Concept
import org.lionweb.lioncore.java.metamodel.ConceptInterface
import org.lionweb.lioncore.java.metamodel.Containment
import org.lionweb.lioncore.java.metamodel.Enumeration
import org.lionweb.lioncore.java.metamodel.EnumerationLiteral
import org.lionweb.lioncore.java.metamodel.FeaturesContainer
import org.lionweb.lioncore.java.metamodel.LionCoreBuiltins
import org.lionweb.lioncore.java.metamodel.Metamodel
import org.lionweb.lioncore.java.metamodel.Property
import org.lionweb.lioncore.java.metamodel.Reference
import org.lionweb.lioncore.java.serialization.JsonSerialization
import org.lionweb.lioncore.java.serialization.PrimitiveValuesSerialization.PrimitiveSerializer
import java.lang.IllegalStateException
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmName

private val conceptsMemory = HashMap<KClass<out ASTNode>, Concept>()
private val enumsMemory = HashMap<KClass<out Enum<*>>, Enumeration>()
private val conceptInterfacesMemory = HashMap<KClass<out Any>, ConceptInterface>()

open class ReflectionBasedMetamodel(id: String, name: String, version: Int, vararg val classes: KClass<*>) : Metamodel() {
    private val consideredClasses = mutableSetOf<KClass<*>>()
    private val enumClasses = mutableSetOf<KClass<out Enum<*>>>()

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
    }

    private var initDone = false

    fun ensureInit() {
        if (initDone) {
            return
        }
        classes.forEach { considerClass(it) }
        initDone = true
    }

    constructor(id: String, name: String, vararg classes: KClass<*>) : this(id, name, 1, *classes)

    fun prepareSerialization(jsonSerialization: JsonSerialization) {
        enumClasses.forEach { enumClass ->
            val enumDeclaration = enumClass.enumeration
            jsonSerialization.primitiveValuesSerialization.registerSerializer(enumDeclaration.id,
                PrimitiveSerializer<Enum<*>> { it.name })
        }
    }

    protected fun considerClass(kClass: KClass<*>) {
        try {
            if (consideredClasses.contains(kClass) || kClass == ASTNode::class || kClass == Any::class) {
                return
            }
            if (kClass.allSuperclasses.contains(Enum::class)) {
                enumClasses.add(kClass as KClass<out Enum<*>>)
                kClass.enumeration
                consideredClasses.add(kClass)
                return
            }
            consideredClasses.add(kClass)
            kClass.nodeProperties.forEach {
                val provideNodes = PropertyDescription.providesNodes(it as KProperty1<in ASTNode, *>)
                val ref = (it as KProperty1<in ASTNode, *>).isReference
                when {
                    !provideNodes -> {
                        if (it.returnType.classifier == ReferenceByName::class) {
                            considerClass(it.returnType.arguments[0].type!!
                                .classifier as KClass<*>)
                        } else {
                            when (it.returnType.classifier) {
                                Boolean::class, String::class,Int::class,Position::class,Char::class -> Unit // nothing to do
                                else -> {
                                    if ((it.returnType.classifier as? KClass<*>)?.allSuperclasses?.contains(Enum::class)
                                            ?: false
                                    ) {
                                        val enum : KClass<out Enum<*>> = it.returnType.classifier as KClass<out Enum<*>>
                                        considerClass(enum)
                                    } else {
                                        if ((it.returnType.classifier as? KClass<*>)?.allSuperclasses?.contains(java.util.List::class)
                                                ?: false) {
                                            throw RuntimeException("Illegal property of lists at $it")
                                        } else {
                                            TODO(
                                                "Return type: ${it.returnType.classifier} " +
                                                        "(${it.returnType.classifier?.javaClass} for property $it"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    provideNodes && ref -> TODO()
                    provideNodes && !ref -> {
                        val containment = Containment()
                        containment.simpleName = it.name
                        containment.id = it.name
                        if ((it.returnType.classifier as KClass<*>).allSupertypes.map { it.classifier }
                                .contains(Collection::class)
                        ) {
                            considerClass(it.returnType.arguments[0].type!!
                                .classifier as KClass<*>)
                        } else {
                            val classifier = it.returnType.classifier
                            considerClass(classifier as KClass<*>)
                        }
                    }
                }
            }
            kClass.superclasses.forEach { considerClass(it) }
            kClass.sealedSubclasses.forEach { considerClass(it) }
            if (kClass.java.isInterface) {
                kClass.conceptInterface
            } else {
                (kClass as? KClass<out ASTNode>)?.concept
            }
        } catch (e: Throwable){
            System.err.println("Issue considering $kClass")
            e.printStackTrace()
            throw java.lang.RuntimeException("Issue considering $kClass", e)
        }
    }
}

val <E : Enum<*>>KClass<E>.enumeration: Enumeration
    get() = enumsMemory.getOrPut(this) {
        calculateEnum(this)
    }

val <A : ASTNode>KClass<A>.concept: Concept
    get() = conceptsMemory.getOrPut(this) {
        val metamodelInstance = metamodelFor(this)
        if (metamodelInstance == null) {
            throw IllegalStateException("no valid metamodel found for class $this")
        } else {
            (metamodelInstance as ReflectionBasedMetamodel).ensureInit()
            return calculateConcept(this, conceptsMemory, conceptInterfacesMemory)
//            if (conceptsMemory.containsKey(this)) {
//                return conceptsMemory[this]!!
//            } else {
//                throw IllegalStateException()
//            }
        }
        //calculateConcept(this, conceptsMemory, conceptInterfacesMemory)
        //throw IllegalStateException("We should ensure that the metamodel has been loaded and the concept has been inserted here")
    }

val <A : Any>KClass<A>.conceptInterface: ConceptInterface
    get() = conceptInterfacesMemory.getOrElse(this) {
        calculateConceptInterface(this, conceptsMemory, conceptInterfacesMemory)
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

private fun metamodelFor(kClass: KClass<out Any>): Metamodel? {
    val metamodelInstance: Metamodel? = if (kClass.jvmName.contains("$")) {
        val outerClass = kClass.java.declaringClass.kotlin
        val metamodelKClass = outerClass.nestedClasses.find { it.simpleName == "Metamodel" }
        if (metamodelKClass == null) {
            return metamodelFor(outerClass as KClass<out ASTNode>)
        }
        metamodelKClass?.objectInstance as? Metamodel
    } else {
        val metamodelQName = kClass.qualifiedName!!.removeSuffix(".${kClass.simpleName}") + ".Metamodel"
        val classLoader = kClass.java.classLoader ?: throw IllegalStateException("No class loader for ${kClass.java}")
        val metamodelKClass = try {
            classLoader.loadClass(metamodelQName).kotlin
        } catch (e: ClassNotFoundException) {
            throw RuntimeException("Unable to find the metamodel for Kotlin class $kClass. We looked for Java class $metamodelQName", e)
        }
        if (metamodelKClass == null) {
            throw IllegalStateException("Metamodel class not found")
        }
        val metamodelInstanceRaw = metamodelKClass.staticProperties.find { it.name == "INSTANCE" }?.let { instance ->
            val instanceRaw = instance.get()
            if (instanceRaw !is Metamodel) {
                throw IllegalStateException("value of INSTANCE field for $metamodelKClass is not a Metamodel but it is $instanceRaw")
            }
            instanceRaw as Metamodel
        } ?: try {
            metamodelKClass.objectInstance
        } catch (e: Throwable) {
            throw java.lang.RuntimeException("Unable to get object instance for $metamodelKClass", e)
        }
        if (metamodelInstanceRaw !is Metamodel) {
            throw IllegalStateException("Object instance for $metamodelKClass is not a Metamodel but it is $metamodelInstanceRaw")
        }
        metamodelInstanceRaw as Metamodel
    }
    return metamodelInstance
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
    if (conceptsMemory.containsKey(kClass)) {
        return conceptsMemory[kClass]!!
    }
    try {
        require(!kClass.java.isInterface)
        if (kClass == ASTNode::class) {
            return StarLasuMetamodel.astNode
        }
        require(
            kClass.allSuperclasses.contains(ASTNode::class) || kClass.findAnnotation<NodeType>() != null ||
                kClass.allSuperclasses.any { it.findAnnotation<NodeType>() != null }
        ) {
            "KClass $kClass is not a subclass of ASTNode"
        }
        val concept = Concept()
        concept.simpleName = kClass.simpleName

        val superclasses = kClass.superclasses.toMutableList()
        // require(superclasses.contains(ASTNode::class))
        superclasses.remove(ASTNode::class)
        superclasses.forEach { superclass ->
            if (superclass.java.isInterface) {
                // TODO consider
            } else {
                concept.extendedConcept = (superclass as KClass<out ASTNode>).concept
            }
        }
        if (concept.extendedConcept == null) {
            concept.extendedConcept = StarLasuMetamodel.astNode
        }

        val metamodelInstance: Metamodel = metamodelFor(kClass)
            ?: throw RuntimeException("No Metamodel object for $kClass")
        if (conceptsMemory.containsKey(kClass)) {
            throw IllegalStateException("This should not happen")
        }
        metamodelInstance.addElement(concept)

        concept.id = metamodelInstance.key + "-" + kClass.simpleName
        concept.key = metamodelInstance.key + "-" + kClass.simpleName

        // We need to add it right away because of nodes referring to themselves
        conceptsMemory[kClass] = concept

        kClass.nodeProperties.forEach {
            val provideNodes = PropertyDescription.providesNodes(it as KProperty1<in ASTNode, *>)
            val ref = (it as KProperty1<in ASTNode, *>).isReference
            when {
                !provideNodes -> {
                    if (it.returnType.classifier == ReferenceByName::class) {
                        val reference = Reference()
                        reference.simpleName = it.name
                        reference.id = it.name
                        reference.key = "${concept.key}-${it.name}"
                        reference.type = it.returnType.arguments[0].type!!
                                .classifier!!.asFeaturesContainer()
                        concept.addFeature(reference)
                    } else {
                        val property = Property()
                        property.simpleName = it.name
                        property.id = it.name
                        property.key = "${concept.key}-${it.name}"
                        when (it.returnType.classifier) {
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
                                if ((it.returnType.classifier as? KClass<*>)?.allSuperclasses?.contains(Enum::class)
                                    ?: false
                                ) {
                                    val enum : KClass<out Enum<*>> = it.returnType.classifier as KClass<out Enum<*>>
                                    property.type = enum.enumeration
                                } else {
                                    TODO(
                                        "Return type: ${it.returnType.classifier} " +
                                            "(${it.returnType.classifier?.javaClass} for property $it"
                                    )
                                }
                            }
                        }
                        concept.addFeature(property)
                    }
                }
                provideNodes && ref -> TODO()
                provideNodes && !ref -> {
                    val containment = Containment()
                    containment.simpleName = it.name
                    containment.id = it.name
                    containment.key = "${concept.key}-${it.name}"
                    if ((it.returnType.classifier as KClass<*>).allSupertypes.map { it.classifier }
                        .contains(Collection::class)
                    ) {
                        containment.isMultiple = true
                        containment.type = it.returnType.arguments[0].type!!
                                .classifier!!.asFeaturesContainer()
                    } else {
                        val classifier = it.returnType.classifier
                        containment.type = classifier!!.asFeaturesContainer()
                    }
                    concept.addFeature(containment)
                }
            }
        }
        return concept
    } catch (e: Throwable) {
        throw RuntimeException("Unable to calculate concept for $kClass", e)
    }
}
