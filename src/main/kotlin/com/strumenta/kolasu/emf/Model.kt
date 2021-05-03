package com.strumenta.kolasu.emf

import com.strumenta.kolasu.model.*
import java.io.ByteArrayOutputStream
import java.io.File
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EEnum
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl
import org.emfjson.jackson.resource.JsonResourceFactory
import java.lang.RuntimeException
import kotlin.reflect.full.memberProperties

fun EPackage.getEClass(javaClass: Class<*>): EClass {
    return this.getEClass(javaClass.simpleName)
}

fun EPackage.getEClass(name: String): EClass {
    return (this.eClassifiers.find { it.name == name } ?: throw IllegalArgumentException("Class not found: $name")) as EClass
}

fun EPackage.getEEnum(javaClass: Class<*>): EEnum {
    return (this.eClassifiers.find { it.name == javaClass.simpleName } ?: throw IllegalArgumentException("Class not found: $javaClass")) as EEnum
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

fun Point.toEObject() : EObject {
    val ec = KOLASU_METAMODEL.getEClass("Point")
    val eo = KOLASU_METAMODEL.eFactoryInstance.create(ec)
    eo.eSet(ec.getEStructuralFeature("line"), this.line)
    eo.eSet(ec.getEStructuralFeature("column"), this.column)
    return eo
}

fun Position.toEObject() : EObject {
    val ec = KOLASU_METAMODEL.getEClass("Position")
    val eo = KOLASU_METAMODEL.eFactoryInstance.create(ec)
    eo.eSet(ec.getEStructuralFeature("start"), this.start.toEObject())
    eo.eSet(ec.getEStructuralFeature("end"), this.end.toEObject())
    return eo
}

fun Node.toEObject(ePackage: EPackage): EObject {
    try {
        val ec = ePackage.getEClass(this.javaClass)
        val eo = ePackage.eFactoryInstance.create(ec)
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
                            val childEO = (it as Node).toEObject(ePackage)
                            elist.add(childEO)
                        } catch (e: Exception) {
                            throw RuntimeException("Unable to map to EObject child $it in property $pd of $this", e)
                        }
                    }
                } else {
                    if (pd.value == null) {
                        eo.eSet(esf, null)
                    } else {
                        eo.eSet(esf, (pd.value as Node).toEObject(ePackage))
                    }
                }
            } else {
                if (pd.multiple) {
                    TODO()
                } else {
                    if (pd.value is Enum<*>) {
                        val ee = ePackage.getEEnum(pd.value.javaClass)
                        val eev = ee.getEEnumLiteral(pd.value.name)
                        eo.eSet(esf, eev)
                    } else {
                        // this could be not a primitive value but a value that we mapped to an EClass
                        if (pd.value != null) {
                            val eClass = ePackage.eClassifiers.filterIsInstance<EClass>().find { it.name == pd.value.javaClass.simpleName }
                            if (eClass != null) {
                                val eoValue = pd.value.dataToEObject(ePackage)
                                try {
                                    eo.eSet(esf, eoValue)
                                } catch (t: Throwable) {
                                    throw RuntimeException("Issue setting $esf in $eo to $eoValue", t)
                                }
                            } else if (pd.value is ReferenceByName<*>) {
                                val refEC = KOLASU_METAMODEL.getEClass("ReferenceByName")
                                val refEO = KOLASU_METAMODEL.eFactoryInstance.create(refEC)
                                refEO.eSet(refEC.getEStructuralFeature("name")!!, pd.value.name)
                                // TODO complete
                                eo.eSet(esf, refEO)
                            } else {
                                try{
                                    eo.eSet(esf, pd.value)
                                } catch (e: Exception) {
                                    throw RuntimeException("Unable to set property $pd of $this. Structural feature: $esf", e)
                                }
                            }
                        } else {
                            try{
                                eo.eSet(esf, pd.value)
                            } catch (e: Exception) {
                                throw RuntimeException("Unable to set property $pd of $this. Structural feature: $esf", e)
                            }
                        }
                    }
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

fun EPackage.saveAsJson(jsonFile: File, restoringURI:Boolean=true) {
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
    println(uf)
    val output = ByteArrayOutputStream()
    resource.save(output, null)
    return output.toString(Charsets.UTF_8.name())
}
