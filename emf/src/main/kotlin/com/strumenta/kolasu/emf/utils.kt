package com.strumenta.kolasu.emf

import org.eclipse.emf.ecore.EObject

fun EObject.setStringAttribute(propertyName: String, propertyValue: String) {
    val structuralFeature = this.eClass().eAllStructuralFeatures.find { it.name == propertyName }!!
    this.eSet(structuralFeature, propertyValue)
}

fun EObject.setSingleContainment(propertyName: String, propertyValue: EObject) {
    val structuralFeature = this.eClass().eAllStructuralFeatures.find { it.name == propertyName }!!
    this.eSet(structuralFeature, propertyValue)
}
