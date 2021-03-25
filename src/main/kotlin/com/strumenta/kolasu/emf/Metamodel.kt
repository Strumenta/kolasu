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

    val result = ePackage.createEClass("Result").apply {
        val rootContainment = EcoreFactory.eINSTANCE.createEReference()
        rootContainment.name = "root"
        rootContainment.eType = EcorePackage.eINSTANCE.eObject
        rootContainment.isContainment = true
        rootContainment.lowerBound = 0
        rootContainment.upperBound = 1
        this.eStructuralFeatures.add(rootContainment)

        addContainment("issues", issue, 0, -1)
    }

    return ePackage
}

class MetamodelBuilder(packageName: String, nsURI: String, nsPrefix: String) {

    private val ePackage: EPackage
    private val eClasses = HashMap<KClass<*>, EClass>()
    private val dataTypes = HashMap<KType, EDataType>()

    init {
        ePackage = EcoreFactory.eINSTANCE.createEPackage()
        ePackage.name = packageName
        ePackage.nsURI = nsURI
        ePackage.nsPrefix = nsPrefix
        ePackage.setResourceURI(nsURI)
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
                (ktype.classifier as? KClass<*>)?.isSubclassOf(Enum::class) == true -> {
                    eDataType = createEEnum(ktype.classifier as KClass<out Enum<*>>)
                }
                else -> {
                    TODO(ktype.toString())
                }
            }
            ePackage.eClassifiers.add(eDataType)
            dataTypes[ktype] = eDataType
        }
        return dataTypes[ktype]!!
    }

    private fun toEClass(kClass: KClass<*>): EClass {
        val eClass = EcoreFactory.eINSTANCE.createEClass()
        kClass.superclasses.forEach {
            if (it != Any::class && it != Node::class) {
                eClass.eSuperTypes.add(addClass(it))
            }
        }
        eClass.name = kClass.simpleName
        eClass.isAbstract = kClass.isAbstract || kClass.isSealed
        kClass.java.processProperties {
            if (it.provideNodes) {
                val ec = EcoreFactory.eINSTANCE.createEReference()
                ec.name = it.name
                if (it.multiple) {
                    ec.lowerBound = 0
                    ec.upperBound = -1
                } else {
                    ec.lowerBound = 0
                    ec.upperBound = 1
                }
                ec.isContainment = true
                ec.eType = addClass(it.valueType.classifier as KClass<*>)
                eClass.eStructuralFeatures.add(ec)
            } else {
                val ea = EcoreFactory.eINSTANCE.createEAttribute()
                ea.name = it.name
                if (it.multiple) {
                    ea.lowerBound = 0
                    ea.upperBound = -1
                } else {
                    ea.lowerBound = 0
                    ea.upperBound = 1
                }
                ea.eType = toEDataType(it.valueType)
                eClass.eStructuralFeatures.add(ea)
            }
        }
        return eClass
    }

    fun addClass(kClass: KClass<*>): EClass {
        if (!eClasses.containsKey(kClass)) {
            val eClass = toEClass(kClass)
            ePackage.eClassifiers.add(eClass)
            eClasses[kClass] = eClass
            if (kClass.isSealed) {
                kClass.sealedSubclasses.forEach { addClass(it) }
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
