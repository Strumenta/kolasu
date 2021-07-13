package com.strumenta.kolasu.emf

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.strumenta.kolasu.model.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.RuntimeException
import kotlin.reflect.full.memberProperties
import kotlin.reflect.KClass
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.*
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl
import org.emfjson.jackson.resource.JsonResourceFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.RuntimeException
import kotlin.reflect.full.memberProperties

fun EPackage.getEClass(javaClass: Class<*>): EClass {
    return this.getEClass(javaClass.eClassifierName)
}

fun EPackage.getEClass(name: String): EClass {
    return (this.eClassifiers.find { it.name == name } ?: throw IllegalArgumentException("Class not found: $name"))
        as EClass
}

fun EPackage.getEEnum(javaClass: Class<*>): EEnum {
    return (
        this.eClassifiers.find { it.name == javaClass.eClassifierName } ?: throw IllegalArgumentException(
            "Class not found: $javaClass"
        )
        ) as EEnum
}

fun Any.dataToEObject(ePackage: EPackage): EObject {
    val ec = ePackage.getEClass(this.javaClass)
    val eo = ePackage.eFactoryInstance.create(ec)
    ec.eAllAttributes.forEach { attr ->
        val prop = this.javaClass.kotlin.memberProperties.find { it.name == attr.name }
        if (prop != null) {
            val value = prop.getValue(this, prop)
            eo.eSet(attr, value)
        } else {
            throw RuntimeException("Unable to set attribute $attr in $this")
        }
    }
    return eo
}

fun Point.toEObject(): EObject {
    val ec = KOLASU_METAMODEL.getEClass("Point")
    val eo = KOLASU_METAMODEL.eFactoryInstance.create(ec)
    eo.eSet(ec.getEStructuralFeature("line"), this.line)
    eo.eSet(ec.getEStructuralFeature("column"), this.column)
    return eo
}

fun Position.toEObject(): EObject {
    val ec = KOLASU_METAMODEL.getEClass("Position")
    val eo = KOLASU_METAMODEL.eFactoryInstance.create(ec)
    eo.eSet(ec.getEStructuralFeature("start"), this.start.toEObject())
    eo.eSet(ec.getEStructuralFeature("end"), this.end.toEObject())
    return eo
}

private fun toValue(ePackage: EPackage, value: Any?, pd: PropertyDescription, esf: EStructuralFeature): Any? {
    val pdValue: Any? = value
    if (pdValue is Enum<*>) {
        val ee = ePackage.getEEnum(pdValue.javaClass)
        return ee.getEEnumLiteral(pdValue.name)
    } else {
        // this could be not a primitive value but a value that we mapped to an EClass
        if (pdValue != null) {
            val eClass = ePackage.eClassifiers.filterIsInstance<EClass>().find {
                it.name == pdValue!!.javaClass.simpleName
            }
            when {
                eClass != null -> {
                    val eoValue = pdValue.dataToEObject(ePackage)
                    try {
                        return eoValue
                    } catch (t: Throwable) {
                        throw RuntimeException("Issue setting $esf to $eoValue", t)
                    }
                }
                pdValue is ReferenceByName<*> -> {
                    val refEC = KOLASU_METAMODEL.getEClass("ReferenceByName")
                    val refEO = KOLASU_METAMODEL.eFactoryInstance.create(refEC)
                    refEO.eSet(refEC.getEStructuralFeature("name")!!, pdValue.name)
                    // TODO complete
                    return refEO
                }
                else -> {
                    try {
                        return pdValue
                    } catch (e: Exception) {
                        throw RuntimeException("Unable to set property $pd. Structural feature: $esf", e)
                    }
                }
            }
        } else {
            try {
                return pdValue
            } catch (e: Exception) {
                throw RuntimeException("Unable to set property $pd. Structural feature: $esf", e)
            }
        }
    }
}

fun packageName(klass: KClass<*>): String =
    klass.qualifiedName!!.substring(0, klass.qualifiedName!!.lastIndexOf("."))

fun EPackage.findEClass(klass: KClass<*>): EClass? {
    return this.findEClass(klass.eClassifierName)
}

fun EPackage.findEClass(name: String): EClass? {
    return this.eClassifiers.find { it is EClass && it.name == name } as EClass?
}

fun Resource.findEClass(klass: KClass<*>): EClass? {
    val ePackage = this.contents.find { it is EPackage && it.name == packageName(klass) } as EPackage?
    return ePackage?.findEClass(klass)
}

fun Resource.getEClass(klass: KClass<*>): EClass = this.findEClass(klass) ?: throw ClassNotFoundException(klass.qualifiedName)

fun Node.toEObject(ePackage: EPackage): EObject {
    return this.toEObject(ePackage.eResource())
}

fun Node.toEObject(eResource: Resource): EObject {
    try {
        val ec = eResource.getEClass(this::class)
        val eo = ec.ePackage.eFactoryInstance.create(ec)
        val astNode = KOLASU_METAMODEL.getEClass("ASTNode")
        val position = astNode.getEStructuralFeature("position")
        val positionValue = this.position?.toEObject()
        eo.eSet(position, positionValue)
        this.processProperties { pd ->
            val esf = ec.eAllStructuralFeatures.find { it.name == pd.name }!!
            if (pd.provideNodes) {
                if (pd.multiple) {
                    val elist = eo.eGet(esf) as MutableList<EObject>
                    (pd.value as List<*>).forEach {
                        try {
                            val childEO = (it as Node).toEObject(eResource)
                            elist.add(childEO)
                        } catch (e: Exception) {
                            throw RuntimeException("Unable to map to EObject child $it in property $pd of $this", e)
                        }
                    }
                } else {
                    if (pd.value == null) {
                        eo.eSet(esf, null)
                    } else {
                        eo.eSet(esf, (pd.value as Node).toEObject(eResource))
                    }
                }
            } else {
                if (pd.multiple) {
                    val elist = eo.eGet(esf) as MutableList<Any>
                    (pd.value as List<*>).forEach {
                        try {
                            val childValue = toValue(ec.ePackage, it, pd, esf)
                            elist.add(childValue!!)
                        } catch (e: Exception) {
                            throw RuntimeException("Unable to map to EObject child $it in property $pd of $this", e)
                        }
                    }
                } else {
                    eo.eSet(esf, toValue(ec.ePackage, pd.value, pd, esf))
                }
            }
        }
        return eo
    } catch (e: Exception) {
        throw RuntimeException("Unable to map to EObject $this", e)
    }
}

fun EObject.saveXMI(xmiFile: File) {
    val resourceSet = ResourceSetImpl()
    resourceSet.resourceFactoryRegistry.extensionToFactoryMap["xmi"] = XMIResourceFactoryImpl()
    val uri: URI = URI.createFileURI(xmiFile.absolutePath)
    val resource: Resource = resourceSet.createResource(uri)
    resource.contents.add(this)
    resource.save(null)
}

fun EPackage.saveAsJson(jsonFile: File, restoringURI: Boolean = true) {
    val startURI = this.eResource().uri
    (this as EObject).saveAsJson(jsonFile)
    if (restoringURI) {
        this.setResourceURI(startURI.toString())
    }
}

fun EObject.saveAsJson(jsonFile: File) {
    val resourceSet = ResourceSetImpl()
    resourceSet.resourceFactoryRegistry.extensionToFactoryMap["json"] = JsonResourceFactory()
    val uri: URI = URI.createFileURI(jsonFile.absolutePath)
    val resource: Resource = resourceSet.createResource(uri)
    resource.contents.add(this)
    resource.save(null)
}

fun EObject.saveAsJson(): String {
    val uri: URI = URI.createURI("dummy-URI")
    val resource: Resource = JsonResourceFactory().createResource(uri)
    resource.contents.add(this)
    val uf = resource.getURIFragment(this)
    val output = ByteArrayOutputStream()
    resource.save(output, null)
    return output.toString(Charsets.UTF_8.name())
}

fun EObject.saveAsJsonObject(): JsonObject {
    return JsonParser().parse(this.saveAsJson()).asJsonObject
}
