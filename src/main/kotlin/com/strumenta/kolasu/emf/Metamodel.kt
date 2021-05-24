package com.strumenta.kolasu.emf

import com.strumenta.kolasu.model.*
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.*
import org.eclipse.emf.ecore.*
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

interface EDataTypeHandler {
    fun canHandle(ktype: KType): Boolean
    fun toDataType(ktype: KType): EDataType
    fun external(): Boolean
}

interface EClassTypeHandler {
    fun canHandle(ktype: KType): Boolean {
        return if (ktype.classifier is KClass<*>) {
            canHandle(ktype.classifier as KClass<*>)
        } else {
            false
        }
    }
    fun canHandle(kclass: KClass<*>): Boolean
    fun toEClass(kclass: KClass<*>, eClassProvider: ClassifiersProvider): EClass
    fun external(): Boolean
}

interface ClassifiersProvider {
    fun isDataType(ktype: KType): Boolean {
        try {
            provideDataType(ktype)
            return true
        } catch (e: Exception) {
            return false
        }
    }
    fun provideClass(kClass: KClass<*>): EClass
    fun provideDataType(ktype: KType): EDataType?
}

// object StandardEClassHandler : EClassTypeHandler {
//        override fun canHandle(kclass: KClass<*>): Boolean {
//            if (kclass == Named::class) {
//                return true
//            } else if (kclass == String::class) {
//                return false
//            } else if (kclass == Boolean::class) {
//                return false
//            } else if (kclass == Int::class) {
//                return false
//            } else if (kclass == ReferenceByName::class) {
//                return false
//            } else {
//                //TODO("Not yet implemented")
//                return true
//            }
//        }
//
//        override fun toEClass(kclass: KClass<*>, classifiersProvider: ClassifiersProvider): EClass {
//            if (kclass == Named::class) {
//                return KOLASU_METAMODEL.getEClass(Named::class.java)
//            } else {
//                val ec = EcoreFactory.eINSTANCE.createEClass()
//                ec.isAbstract = kclass.isSealed || kclass.isAbstract
//                ec.isInterface = kclass.java.isInterface
//                ec.name = kclass.simpleName
//                kclass.supertypes.forEach {
//                    if (it == Any::class.createType()) {
//                        // ignore
//                    } else {
//                        val parent = classifiersProvider.provideClass(it.classifier as KClass<*>)
//                        ec.eSuperTypes.add(parent)
//                    }
//                }
//                kclass.memberProperties.forEach {
//                    val isDerived = it.annotations.any { it is Derived }
//
//                    if (!isDerived) {
//                        val isMany = it.returnType.isSubtypeOf(Collection::class.createType(listOf(KTypeProjection.STAR)))
//                        val baseType = if (isMany) it.returnType.arguments[0].type!! else it.returnType
//                        if (baseType.classifier == ReferenceByName::class){
//                            TODO()
//                        }
//                        val isAttr = classifiersProvider.isDataType(baseType)
//                        if (isAttr) {
//                            val ea = EcoreFactory.eINSTANCE.createEAttribute()
//                            ea.name = it.name
//                            if (isMany) {
//                                ea.upperBound = -1
//                                ea.lowerBound = 0
//                            }
//                            ea.eType = classifiersProvider.provideDataType(baseType)
//                            ec.eStructuralFeatures.add(ea)
//                        } else {
//                            val er = EcoreFactory.eINSTANCE.createEReference()
//                            er.name = it.name
//                            if (isMany) {
//                                er.upperBound = -1
//                                er.lowerBound = 0
//                            }
//                            er.isContainment = true
//                            er.eType = classifiersProvider.provideClass(baseType.classifier as KClass<*>)
//                            ec.eStructuralFeatures.add(er)
//                        }
//                    }
//                }
//                return ec
//                //TODO("Not yet implemented")
//            }
//        }
//
//    }

class KolasuClassHandler(val kolasuKClass: KClass<*>, val kolasuEClass: EClass) : EClassTypeHandler {
    override fun canHandle(kclass: KClass<*>): Boolean = kclass == kolasuKClass

    override fun toEClass(kclass: KClass<*>, eClassProvider: ClassifiersProvider): EClass {
        return kolasuEClass
    }

    override fun external(): Boolean = true
}

class KolasuDataTypeHandler(val kolasuKClass: KClass<*>, val kolasuDataType: EDataType) : EDataTypeHandler {
    override fun canHandle(ktype: KType): Boolean {
        return ktype == kolasuKClass.createType()
    }

    override fun toDataType(ktype: KType): EDataType {
        return kolasuDataType
    }

    override fun external(): Boolean = true
}

val NodeHandler = KolasuClassHandler(Node::class, KOLASU_METAMODEL.getEClass("ASTNode"))
val NamedHandler = KolasuClassHandler(Named::class, KOLASU_METAMODEL.getEClass("Named"))
val ReferenceByNameHandler = KolasuClassHandler(ReferenceByName::class, KOLASU_METAMODEL.getEClass("ReferenceByName"))

val StringHandler = KolasuDataTypeHandler(String::class, KOLASU_METAMODEL.getEClassifier("string") as EDataType)
val BooleanHandler = KolasuDataTypeHandler(Boolean::class, KOLASU_METAMODEL.getEClassifier("boolean") as EDataType)
val IntHandler = KolasuDataTypeHandler(Int::class, KOLASU_METAMODEL.getEClassifier("int") as EDataType)
val BigIntegerHandler = KolasuDataTypeHandler(BigInteger::class, KOLASU_METAMODEL.getEClassifier("BigInteger") as EDataType)
val BigDecimalHandler = KolasuDataTypeHandler(BigDecimal::class, KOLASU_METAMODEL.getEClassifier("BigDecimal") as EDataType)
val LongHandler = KolasuDataTypeHandler(Long::class, KOLASU_METAMODEL.getEClassifier("long") as EDataType)

val KClass<*>.eClassifierName
    get() = this.java.eClassifierName

val Class<*>.eClassifierName
    get() = if (this.enclosingClass != null) {
        "${this.enclosingClass.simpleName}${this.simpleName}"
    } else {
        this.simpleName
    }

class MetamodelBuilder(packageName: String, nsURI: String, nsPrefix: String) : ClassifiersProvider {

    private val ePackage: EPackage
    private val eClasses = HashMap<KClass<*>, EClass>()
    private val dataTypes = HashMap<KType, EDataType>()
    private val eclassTypeHandlers = LinkedList<EClassTypeHandler>()
    private val dataTypeHandlers = LinkedList<EDataTypeHandler>()

    init {
        ePackage = EcoreFactory.eINSTANCE.createEPackage()
        ePackage.name = packageName
        ePackage.nsURI = nsURI
        ePackage.nsPrefix = nsPrefix
        ePackage.setResourceURI(nsURI)

        dataTypeHandlers.add(StringHandler)
        dataTypeHandlers.add(BooleanHandler)
        dataTypeHandlers.add(IntHandler)
        dataTypeHandlers.add(LongHandler)
        dataTypeHandlers.add(BigIntegerHandler)
        dataTypeHandlers.add(BigDecimalHandler)

        eclassTypeHandlers.add(NodeHandler)
        eclassTypeHandlers.add(NamedHandler)
        eclassTypeHandlers.add(ReferenceByNameHandler)
    }

    /**
     * Normally a class is not treated as a DataType, so we need specific DataTypeHandlers
     * to recognize it as such
     */
    fun addDataTypeHandler(eDataTypeHandler: EDataTypeHandler) {
        dataTypeHandlers.add(eDataTypeHandler)
    }

    /**
     * This should be needed only to customize how we want to deal with a class when translating
     * it to an EClass
     */
    fun addEClassTypeHandler(eClassTypeHandler: EClassTypeHandler) {
        eclassTypeHandlers.add(eClassTypeHandler)
    }

    private fun createEEnum(kClass: KClass<out Enum<*>>): EEnum {
        val eEnum = EcoreFactory.eINSTANCE.createEEnum()
        eEnum.name = kClass.eClassifierName
        kClass.java.enumConstants.forEach {
            var eLiteral = EcoreFactory.eINSTANCE.createEEnumLiteral()
            eLiteral.name = it.name
            eLiteral.value = it.ordinal
            eEnum.eLiterals.add(eLiteral)
        }
        return eEnum
    }

    override fun provideDataType(ktype: KType): EDataType? {
        if (!dataTypes.containsKey(ktype)) {
            var eDataType = EcoreFactory.eINSTANCE.createEDataType()
            var external = false
            when {
                (ktype.classifier as? KClass<*>)?.isSubclassOf(Enum::class) == true -> {
                    eDataType = createEEnum(ktype.classifier as KClass<out Enum<*>>)
                }
                else -> {
                    val handler = dataTypeHandlers.find { it.canHandle(ktype) }
                    if (handler == null) {
                        // throw RuntimeException("Unable to handle data type $ktype, with classifier ${ktype.classifier}")\
                        return null
                    } else {
                        external = handler.external()
                        eDataType = handler.toDataType(ktype)
                    }
                }
            }
            if (!external) {
                ensureClassifierNameIsNotUsed(eDataType)
                ePackage.eClassifiers.add(eDataType)
            }
            dataTypes[ktype] = eDataType
        }
        return dataTypes[ktype]!!
    }

    private fun classToEClass(kClass: KClass<*>): EClass {
        val eClass = EcoreFactory.eINSTANCE.createEClass()
        // This is necessary because some classes refer to themselves
        registerKClassForEClass(kClass, eClass)

        kClass.superclasses.forEach {
            if (it != Any::class) {
                eClass.eSuperTypes.add(provideClass(it))
            }
        }
        eClass.name = kClass.eClassifierName

        eClass.isAbstract = kClass.isAbstract || kClass.isSealed

        kClass.java.processProperties { prop ->
            try {
                if (eClass.eAllStructuralFeatures.any { sf -> sf.name == prop.name }) {
                    // skip
                } else {
                    // do not process inherited properties
                    if (prop.provideNodes) {
                        val ec = EcoreFactory.eINSTANCE.createEReference()
                        ec.name = prop.name
                        if (prop.multiple) {
                            ec.lowerBound = 0
                            ec.upperBound = -1
                        } else {
                            ec.lowerBound = 0
                            ec.upperBound = 1
                        }
                        ec.isContainment = true
                        ec.eType = provideClass(prop.valueType.classifier as KClass<*>)
                        eClass.eStructuralFeatures.add(ec)
//                    } else if (prop.valueType.classifier == ReferenceByName::class) {
//                        val ec = EcoreFactory.eINSTANCE.createEReference()
//                        ec.name = prop.name
//                        ec.isContainment = true
//                        ec.eGenericType = EcoreFactory.eINSTANCE.createEGenericType().apply {
//                            this.eClassifier = KOLASU_METAMODEL.getEClass(ReferenceByName::class.java)
//                            val eClassForReferenced : EClass = provideClass(prop.valueType.arguments[0].type!!.classifier!! as KClass<*>)
//                            this.eTypeArguments.add(EcoreFactory.eINSTANCE.createEGenericType().apply {
//                                this.eClassifier = eClassForReferenced
//                            })
//                        }
//                        eClass.eStructuralFeatures.add(ec)
                    } else {
                        val nullable = prop.valueType.isMarkedNullable
                        val dataType = provideDataType(prop.valueType.withNullability(false))
                        if (dataType == null) {
                            // We can treat it like a class
                            val eContainment = EcoreFactory.eINSTANCE.createEReference()
                            eContainment.name = prop.name
                            if (prop.multiple) {
                                eContainment.lowerBound = 0
                                eContainment.upperBound = -1
                            } else {
                                eContainment.lowerBound = 0
                                eContainment.upperBound = 1
                            }
                            eContainment.isContainment = true
                            eContainment.eType = provideClass(prop.valueType.classifier as KClass<*>)
                            eClass.eStructuralFeatures.add(eContainment)
                        } else {
                            val ea = EcoreFactory.eINSTANCE.createEAttribute()
                            ea.name = prop.name
                            if (prop.multiple) {
                                ea.lowerBound = 0
                                ea.upperBound = -1
                            } else {
                                ea.lowerBound = if (nullable) 0 else 1
                                ea.upperBound = 1
                            }
                            ea.eType = dataType
                            eClass.eStructuralFeatures.add(ea)
                        }
                    }
                }
            } catch (e: Exception) {
                throw RuntimeException("Issue processing property $prop in class $kClass", e)
            }
        }
        return eClass
    }

    private fun ensureClassifierNameIsNotUsed(classifier: EClassifier) {
        if (ePackage.hasClassifierNamed(classifier.name)) {
            throw IllegalStateException("There is already a Classifier named ${classifier.name}: ${ePackage.classifierByName(classifier.name)}")
        }
    }

    private fun registerKClassForEClass(kClass: KClass<*>, eClass: EClass) {
        if (eClasses.containsKey(kClass)) {
            require(eClasses[kClass] == eClass)
        } else {
            eClasses[kClass] = eClass
        }
    }

    override fun provideClass(kClass: KClass<*>): EClass {
        if (!eClasses.containsKey(kClass)) {
            val ch = eclassTypeHandlers.find { it.canHandle(kClass) }
            val eClass = ch?.toEClass(kClass, this) ?: classToEClass(kClass)
            if (ch == null || !ch.external()) {
                ensureClassifierNameIsNotUsed(eClass)
                ePackage.eClassifiers.add(eClass)
            }
            registerKClassForEClass(kClass, eClass)
            if (kClass.isSealed) {
                kClass.sealedSubclasses.forEach {
                    //provideClass(it)
                    queue.add(it)
                }
            }
        }
        while (queue.isNotEmpty()) {
            provideClass(queue.removeFirst())
        }
        return eClasses[kClass]!!
    }

    private val queue = LinkedList<KClass<*>>()

    fun generate(): EPackage {
        return ePackage
    }
}


private fun EPackage.hasClassifierNamed(name: String): Boolean {
    return this.eClassifiers.any { it.name == name }
}

private fun EPackage.classifierByName(name: String): EClassifier {
    return this.eClassifiers.find { it.name == name } ?: throw IllegalArgumentException("No classifier named $name was found")
}
