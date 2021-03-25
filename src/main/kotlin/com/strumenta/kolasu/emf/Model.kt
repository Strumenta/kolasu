package com.strumenta.kolasu.emf

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.processProperties
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

fun EPackage.getEClass(javaClass: Class<*>): EClass {
    return (this.eClassifiers.find { it.name == javaClass.simpleName } ?: throw IllegalArgumentException("Class not found: $javaClass")) as EClass
}

fun EPackage.getEEnum(javaClass: Class<*>): EEnum {
    return (this.eClassifiers.find { it.name == javaClass.simpleName } ?: throw IllegalArgumentException("Class not found: $javaClass")) as EEnum
}

fun Node.toEObject(ePackage: EPackage): EObject {
    val ec = ePackage.getEClass(this.javaClass)
    val eo = ePackage.eFactoryInstance.create(ec)
    this.processProperties { pd ->
        val esf = ec.eAllStructuralFeatures.find { it.name == pd.name }!!
        if (pd.provideNodes) {
            if (pd.multiple) {
                val elist = eo.eGet(esf) as MutableList<EObject>
                (pd.value as List<*>).forEach {
                    val childEO = (it as Node).toEObject(ePackage)
                    elist.add(childEO)
                }
            } else {
                eo.eSet(esf, (pd.value as Node).toEObject(ePackage))
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
                    eo.eSet(esf, pd.value)
                }
            }
        }
    }
    return eo
}

fun EObject.saveXMI(xmiFile: File) {
    val resourceSet = ResourceSetImpl()
    resourceSet.resourceFactoryRegistry.extensionToFactoryMap["xmi"] = XMIResourceFactoryImpl()
    val uri: URI = URI.createFileURI(xmiFile.absolutePath)
    val resource: Resource = resourceSet.createResource(uri)
    resource.contents.add(this)
    resource.save(null)
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
