package com.strumenta.kolasu.emf

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.processProperties
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.*
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl
import org.emfjson.jackson.resource.JsonResourceFactory
import java.io.File
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.jvmErasure

class MetamodelBuilder(packageName: String, nsURI: String) {

    private val ePackage : EPackage
    private val eClasses = HashMap<KClass<*>, EClass>()
    private val dataTypes = HashMap<KType, EDataType>()

    init {
        ePackage = EcoreFactory.eINSTANCE.createEPackage()
        ePackage.name = packageName
        ePackage.nsURI = nsURI
    }

    private fun toEDataType(ktype: KType) : EDataType {
        if (!dataTypes.containsKey(ktype)) {
            val eDataType = EcoreFactory.eINSTANCE.createEDataType()
            if (ktype.classifier == String::class) {
                eDataType.name = "String"
                eDataType.instanceClass = String::class.java
            } else {
                TODO(ktype.toString())
            }
            ePackage.eClassifiers.add(eDataType)
            dataTypes[ktype] = eDataType
        }
        return dataTypes[ktype]!!
    }

    private fun toEClass(kClass: KClass<*>) : EClass {
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
                eClass.eReferences.add(ec)
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
                eClass.eAttributes.add(ea)
            }
        }
        return eClass
    }

    fun addClass(kClass: KClass<*>) : EClass {
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

    fun generate() : EPackage {
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
