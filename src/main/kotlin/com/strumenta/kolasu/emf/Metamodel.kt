package com.strumenta.kolasu.emf

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.processProperties
import com.strumenta.kolasu.validation.IssueType
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.superclasses
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.*
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl
import java.lang.RuntimeException
import java.util.*
import kotlin.collections.HashMap

fun EEnum.addLiteral(enumEntry: Enum<*>) {
    this.addLiteral(enumEntry.name)
}

fun EEnum.addLiteral(name: String) {
    val newLiteral = EcoreFactory.eINSTANCE.createEEnumLiteral()
    newLiteral.name = name
    newLiteral.value = this.eLiterals.size
    this.eLiterals.add(newLiteral)
}

fun EPackage.createEClass(name: String): EClass {
    val eClass = EcoreFactory.eINSTANCE.createEClass()
    eClass.name = name
    this.eClassifiers.add(eClass)
    return eClass
}

fun EClass.addContainment(name: String, type: EClass, min: Int, max: Int): EReference {
    val eReference = EcoreFactory.eINSTANCE.createEReference()
    eReference.isContainment = true
    eReference.name = name
    eReference.eType = type
    eReference.lowerBound = min
    eReference.upperBound = max
    this.eStructuralFeatures.add(eReference)
    return eReference
}

fun EClass.addAttribute(name: String, type: EDataType, min: Int, max: Int): EAttribute {
    val eAttribute = EcoreFactory.eINSTANCE.createEAttribute()
    eAttribute.name = name
    eAttribute.eType = type
    eAttribute.lowerBound = min
    eAttribute.upperBound = max
    this.eStructuralFeatures.add(eAttribute)
    return eAttribute
}

val KOLASU_METAMODEL by lazy { createKolasuMetamodel() }

fun EPackage.setResourceURI(uri: String) {
    val resource = EcoreResourceFactoryImpl().createResource(URI.createURI(uri))
    resource.contents.add(this)
}

fun createKolasuMetamodel(): EPackage {
    val ePackage = EcoreFactory.eINSTANCE.createEPackage()
    val nsUri = "https://strumenta.com/kolasu"
    ePackage.setResourceURI(nsUri)
    ePackage.name = "StrumentaParser"
    ePackage.nsURI = nsUri

    val intDT = EcoreFactory.eINSTANCE.createEDataType()
    intDT.name = "int"
    intDT.instanceClass = Int::class.java
    ePackage.eClassifiers.add(intDT)

    val stringDT = EcoreFactory.eINSTANCE.createEDataType()
    stringDT.name = "string"
    stringDT.instanceClass = String::class.java
    ePackage.eClassifiers.add(stringDT)

    val point = ePackage.createEClass("Point").apply {
        addAttribute("line", intDT, 1, 1)
        addAttribute("column", intDT, 1, 1)
    }
    val position = ePackage.createEClass("Position").apply {
        addContainment("start", point, 1, 1)
        addContainment("end", point, 1, 1)
    }
    val astNode = ePackage.createEClass("ASTNode").apply {
        this.isAbstract = true
        addContainment("position", position,0, 1)
    }

    val issueType = EcoreFactory.eINSTANCE.createEEnum()
    issueType.name = "IssueType"
    issueType.addLiteral(IssueType.LEXICAL)
    issueType.addLiteral(IssueType.SYNTACTIC)
    issueType.addLiteral(IssueType.SEMANTIC)
    ePackage.eClassifiers.add(issueType)

    val issue = ePackage.createEClass("Issue").apply {
        addAttribute("type", issueType, 1, 1)
        addAttribute("message", stringDT, 1, 1)
        addContainment("position", position, 0, 1)
    }

    val possiblyNamed = ePackage.createEClass("PossiblyNamed").apply {
        isInterface = true

        addAttribute("name", stringDT, 0, 1)
    }
    val named = ePackage.createEClass("Named").apply {
        isInterface = true
        eSuperTypes.add(possiblyNamed)
        addAttribute("name", stringDT, 1, 1)
    }

    val result = ePackage.createEClass("Result").apply {
        val typeParameter = EcoreFactory.eINSTANCE.createETypeParameter().apply {
            this.name = "CU"
            this.eBounds.add(EcoreFactory.eINSTANCE.createEGenericType().apply {
                this.eClassifier = astNode
            })
        }
        this.eTypeParameters.add(typeParameter)
        val rootContainment = EcoreFactory.eINSTANCE.createEReference()
        rootContainment.name = "root"
        rootContainment.eGenericType = EcoreFactory.eINSTANCE.createEGenericType().apply {
            this.eTypeParameter = typeParameter
        }
        rootContainment.isContainment = true
        rootContainment.lowerBound = 0
        rootContainment.upperBound = 1
        this.eStructuralFeatures.add(rootContainment)

        addContainment("issues", issue, 0, -1)
    }

    return ePackage
}

interface EDataTypeHandler {
    fun canHandle(ktype: KType) : Boolean
    fun toDataType(ktype: KType): EDataType
}

object BasicKClassDataTypeHandler : EDataTypeHandler {
    override fun canHandle(ktype: KType): Boolean {
        return ktype.classifier is KClass<*>
    }

    override fun toDataType(ktype: KType): EDataType {
        val kclass = ktype.classifier as KClass<*>
        val eDataType = EcoreFactory.eINSTANCE.createEDataType()
        eDataType.name = kclass.simpleName!!
        eDataType.instanceClass = kclass.java
        return eDataType
    }

}

interface EClassTypeHandler {
    fun canHandle(ktype: KType) : Boolean {
        return if (ktype.classifier is KClass<*>) {
            canHandle(ktype.classifier as KClass<*>)
        } else {
            false
        }
    }
    fun canHandle(kclass: KClass<*>) : Boolean
    fun toEClass(kclass: KClass<*>, eClassProvider: EClassProvider): EClass
}

interface EClassProvider {
    fun provideClass(kClass: KClass<*>): EClass
}

class MetamodelBuilder(packageName: String, nsURI: String, nsPrefix: String) : EClassProvider {

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
    }

    fun addDataTypeHandler(eDataTypeHandler: EDataTypeHandler) {
        dataTypeHandlers.add(eDataTypeHandler)
    }

    fun addEClassTypeHandler(eClassTypeHandler: EClassTypeHandler) {
        eclassTypeHandlers.add(eClassTypeHandler)
    }

    private fun createEEnum(kClass: KClass<out Enum<*>>): EEnum {
        val eEnum = EcoreFactory.eINSTANCE.createEEnum()
        eEnum.name = kClass.simpleName
        kClass.java.enumConstants.forEach {
            var eLiteral = EcoreFactory.eINSTANCE.createEEnumLiteral()
            eLiteral.name = it.name
            eLiteral.value = it.ordinal
            eEnum.eLiterals.add(eLiteral)
        }
        return eEnum
    }

    private fun toEDataType(ktype: KType): EDataType {
        if (!dataTypes.containsKey(ktype)) {
            var eDataType = EcoreFactory.eINSTANCE.createEDataType()
            when {
                ktype.classifier == String::class -> {
                    eDataType.name = "String"
                    eDataType.instanceClass = String::class.java
                }
                ktype.classifier == Boolean::class -> {
                    eDataType.name = "Boolean"
                    eDataType.instanceClass = Boolean::class.java
                }
                (ktype.classifier as? KClass<*>)?.isSubclassOf(Enum::class) == true -> {
                    eDataType = createEEnum(ktype.classifier as KClass<out Enum<*>>)
                }
                else -> {
                    val handler = dataTypeHandlers.find { it.canHandle(ktype) }
                    if (handler == null) {
                        throw RuntimeException("Unable to handle data type $ktype, with classifier ${ktype.classifier}")
                    } else {
                        eDataType = handler.toDataType(ktype)
                    }
                }
            }
            ePackage.eClassifiers.add(eDataType)
            dataTypes[ktype] = eDataType
        }
        return dataTypes[ktype]!!
    }

    private fun nodeClassToEClass(kClass: KClass<*>): EClass {
        val eClass = EcoreFactory.eINSTANCE.createEClass()
        kClass.superclasses.forEach {
            if (it != Any::class && it != Node::class) {
                eClass.eSuperTypes.add(provideClass(it))
            }
        }
        if (eClass.eSuperTypes.isEmpty()) {
            eClass.eSuperTypes.add(KOLASU_METAMODEL.getEClass("ASTNode"))
        }
        eClass.name = kClass.simpleName
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
                    } else {
                        val ch = eclassTypeHandlers.find { it.canHandle(prop.valueType) }
                        if (ch != null) {
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
                                ea.lowerBound = 0
                                ea.upperBound = 1
                            }
                            ea.eType = toEDataType(prop.valueType)
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

    override fun provideClass(kClass: KClass<*>): EClass {
        if (!eClasses.containsKey(kClass)) {
            val eClass = if (kClass.isSubclassOf(Node::class)) {
                nodeClassToEClass(kClass)
            } else {
                eclassTypeHandlers.find { it.canHandle(kClass) }!!.toEClass(kClass, this)
            }
            ePackage.eClassifiers.add(eClass)
            eClasses[kClass] = eClass
            if (kClass.isSealed) {
                kClass.sealedSubclasses.forEach { provideClass(it) }
            }
        }
        return eClasses[kClass]!!
    }

    fun generate(): EPackage {
        return ePackage
    }
}

fun EPackage.saveEcore(ecoreFile: File) {
    val resourceSet = ResourceSetImpl()
    resourceSet.resourceFactoryRegistry.extensionToFactoryMap["ecore"] = EcoreResourceFactoryImpl()
    val uri: URI = URI.createFileURI(ecoreFile.absolutePath)
    val resource: Resource = resourceSet.createResource(uri)
    resource.contents.add(this)
    resource.save(null)
}

fun main(args: Array<String>) {
    KOLASU_METAMODEL.saveEcore(File("kolasu.ecore"))
}