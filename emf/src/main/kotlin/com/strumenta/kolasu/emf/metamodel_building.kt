package com.strumenta.kolasu.emf

import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.*
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

fun EPackage.createEClass(name: String, isAbstract: Boolean = false): EClass {
    val eClass = EcoreFactory.eINSTANCE.createEClass()
    eClass.name = name
    eClass.isAbstract = isAbstract
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

fun EClass.addReference(name: String, type: EClass, min: Int, max: Int): EReference {
    val eReference = EcoreFactory.eINSTANCE.createEReference()
    eReference.isContainment = false
    eReference.name = name
    eReference.eType = type
    eReference.lowerBound = min
    eReference.upperBound = max
    this.eStructuralFeatures.add(eReference)
    return eReference
}

fun EClass.addAttribute(name: String, type: EClassifier, min: Int, max: Int): EAttribute {
    val eAttribute = EcoreFactory.eINSTANCE.createEAttribute()
    eAttribute.name = name
    eAttribute.eType = type
    eAttribute.lowerBound = min
    eAttribute.upperBound = max
    this.eStructuralFeatures.add(eAttribute)
    return eAttribute
}

fun EPackage.setResourceURI(uri: String) {
    val resource = EcoreResourceFactoryImpl().createResource(URI.createURI(uri))
    resource.contents.add(this)
}
