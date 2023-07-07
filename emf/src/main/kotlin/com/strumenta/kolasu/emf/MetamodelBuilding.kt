package com.strumenta.kolasu.emf

import com.strumenta.kolasu.language.KolasuLanguage
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.EAttribute
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EDataType
import org.eclipse.emf.ecore.EEnum
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EReference
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl
import kotlin.reflect.KClass
import kotlin.reflect.full.staticFunctions

fun EEnum.addLiteral(enumEntry: Enum<*>) {
    this.addLiteral(enumEntry.name)
}

fun EEnum.addAllLiterals(enumClass: KClass<out Enum<*>>) {
    val literals = enumClass.staticFunctions.find { it.name == "values" }!!.call() as Array<Enum<*>>
    literals.forEach { addLiteral(it) }
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

fun EClass.addAttribute(name: String, type: EDataType, min: Int, max: Int): EAttribute {
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

fun KolasuLanguage.toEPackage(nsUri: String? = null, nsPrefix: String? = null): EPackage {
    val qualifiedNameParts = this.qualifiedName.split(".")
    val nsUriCalc = nsUri ?: if (qualifiedNameParts.size >= 3) {
        "https://${qualifiedNameParts[1]}.${qualifiedNameParts[0]}/" +
            qualifiedName.removePrefix("${qualifiedNameParts[0]}.${qualifiedNameParts[1]}.")
    } else {
        "https://strumenta.com/${this.qualifiedName}"
    }

    val mmBuilder = MetamodelBuilder(
        this.qualifiedName,
        nsUriCalc,
        nsPrefix ?: this.simpleName
    )
    this.astClasses.forEach {
        mmBuilder.provideClass(it)
    }
    return mmBuilder.generate()
}
