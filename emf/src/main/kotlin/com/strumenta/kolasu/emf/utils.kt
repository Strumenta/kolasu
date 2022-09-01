package com.strumenta.kolasu.emf

import org.eclipse.emf.common.util.EList
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl
import org.eclipse.emfcloud.jackson.resource.JsonResourceFactory

fun EObject.setStringAttribute(propertyName: String, propertyValue: String) {
    val structuralFeature = this.eClass().eAllStructuralFeatures.find { it.name == propertyName }!!
    this.eSet(structuralFeature, propertyValue)
}

fun EObject.setSingleContainment(propertyName: String, propertyValue: EObject) {
    val structuralFeature = this.eClass().eAllStructuralFeatures.find { it.name == propertyName }!!
    this.eSet(structuralFeature, propertyValue)
}

fun EObject.setMultipleContainment(propertyName: String, propertyValue: List<EObject>) {
    val structuralFeature = this.eClass().eAllStructuralFeatures.find { it.name == propertyName }!!
    (this.eGet(structuralFeature) as EList<EObject>).addAll(propertyValue)
}

fun createResourceSet(): ResourceSet {
    val resourceSet: ResourceSet = ResourceSetImpl()
    resourceSet.resourceFactoryRegistry.extensionToFactoryMap["json"] = JsonResourceFactory()
    resourceSet.resourceFactoryRegistry.extensionToFactoryMap["xmi"] = XMIResourceFactoryImpl()
    resourceSet.resourceFactoryRegistry.extensionToFactoryMap["ecore"] = EcoreResourceFactoryImpl()
    return resourceSet
}

fun createResource(uri: URI): Resource? {
    val resourceSet = createResourceSet()
    return resourceSet.createResource(uri)
}
