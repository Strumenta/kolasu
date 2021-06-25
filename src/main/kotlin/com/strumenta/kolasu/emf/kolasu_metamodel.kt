package com.strumenta.kolasu.emf

import com.strumenta.kolasu.validation.IssueType
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EcoreFactory
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger

val KOLASU_METAMODEL by lazy { createKolasuMetamodel() }

fun createKolasuMetamodel(): EPackage {
    val ePackage = EcoreFactory.eINSTANCE.createEPackage()
    val nsUri = "https://strumenta.com/kolasu/v1"
    ePackage.setResourceURI(nsUri)
    ePackage.name = "StrumentaParser"
    ePackage.nsURI = nsUri

    val intDT = EcoreFactory.eINSTANCE.createEDataType()
    intDT.name = "int"
    intDT.instanceClass = Int::class.java
    ePackage.eClassifiers.add(intDT)

    val longDT = EcoreFactory.eINSTANCE.createEDataType()
    longDT.name = "long"
    longDT.instanceClass = Long::class.java
    ePackage.eClassifiers.add(longDT)

    val bigDecimalDT = EcoreFactory.eINSTANCE.createEDataType()
    bigDecimalDT.name = "BigDecimal"
    bigDecimalDT.instanceClass = BigDecimal::class.java
    ePackage.eClassifiers.add(bigDecimalDT)

    val bigIntegerDT = EcoreFactory.eINSTANCE.createEDataType()
    bigIntegerDT.name = "BigInteger"
    bigIntegerDT.instanceClass = BigInteger::class.java
    ePackage.eClassifiers.add(bigIntegerDT)

    val stringDT = EcoreFactory.eINSTANCE.createEDataType()
    stringDT.name = "string"
    stringDT.instanceClass = String::class.java
    ePackage.eClassifiers.add(stringDT)

    val booleanDT = EcoreFactory.eINSTANCE.createEDataType()
    booleanDT.name = "boolean"
    booleanDT.instanceClass = Boolean::class.java
    ePackage.eClassifiers.add(booleanDT)

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
        addContainment("position", position, 0, 1)
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

    val referenceByName = ePackage.createEClass("ReferenceByName").apply {
        val typeParameter = EcoreFactory.eINSTANCE.createETypeParameter().apply {
            this.name = "N"
            this.eBounds.add(
                EcoreFactory.eINSTANCE.createEGenericType().apply {
                    this.eClassifier = astNode
                }
            )
        }
        this.eTypeParameters.add(typeParameter)
        val rootContainment = EcoreFactory.eINSTANCE.createEReference()
        rootContainment.name = "referenced"
        rootContainment.eGenericType = EcoreFactory.eINSTANCE.createEGenericType().apply {
            this.eTypeParameter = typeParameter
        }
        rootContainment.isContainment = true
        rootContainment.lowerBound = 0
        rootContainment.upperBound = 1

        addAttribute("name", stringDT, 1, 1)
        this.eStructuralFeatures.add(rootContainment)
    }

    val result = ePackage.createEClass("Result").apply {
        val typeParameter = EcoreFactory.eINSTANCE.createETypeParameter().apply {
            this.name = "CU"
            this.eBounds.add(
                EcoreFactory.eINSTANCE.createEGenericType().apply {
                    this.eClassifier = astNode
                }
            )
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

fun main(args: Array<String>) {
    KOLASU_METAMODEL.saveEcore(File("kolasu.ecore"))
    KOLASU_METAMODEL.saveAsJson(File("kolasu.json"))
}
