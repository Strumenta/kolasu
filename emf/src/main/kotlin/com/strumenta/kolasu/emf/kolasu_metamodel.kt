package com.strumenta.kolasu.emf

import com.strumenta.kolasu.validation.IssueSeverity
import com.strumenta.kolasu.validation.IssueType
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.emf.ecore.EcorePackage
import java.io.File

val KOLASU_METAMODEL by lazy { createKolasuMetamodel() }
val ASTNODE_ECLASS by lazy { KOLASU_METAMODEL.eClassifiers.find { it.name == "ASTNode" }!! as EClass }

private fun createKolasuMetamodel(): EPackage {
    val ePackage = EcoreFactory.eINSTANCE.createEPackage()
    val nsUri = "https://strumenta.com/starlasu/v2"
    ePackage.setResourceURI(nsUri)
    ePackage.name = "StrumentaLanguageSupport"
    ePackage.nsURI = nsUri

    val intDT = EcorePackage.eINSTANCE.eInt
    val stringDT = EcorePackage.eINSTANCE.eString

    val localDate = ePackage.createEClass("LocalDate").apply {
        addAttribute("year", intDT, 1, 1)
        addAttribute("month", intDT, 1, 1)
        addAttribute("dayOfMonth", intDT, 1, 1)
    }
    val localTime = ePackage.createEClass("LocalTime").apply {
        addAttribute("hour", intDT, 1, 1)
        addAttribute("minute", intDT, 1, 1)
        addAttribute("second", intDT, 1, 1)
        addAttribute("nanosecond", intDT, 1, 1)
    }
    val localDateTime = ePackage.createEClass("LocalDateTime").apply {
        addContainment("date", localDate, 1, 1)
        addContainment("time", localTime, 1, 1)
    }

    val point = ePackage.createEClass("Point").apply {
        addAttribute("line", intDT, 1, 1)
        addAttribute("column", intDT, 1, 1)
    }
    val position = ePackage.createEClass("Position").apply {
        addContainment("start", point, 1, 1)
        addContainment("end", point, 1, 1)
    }
    val origin = ePackage.createEClass("Origin", isAbstract = true)
    val destination = ePackage.createEClass("Destination", isAbstract = true)
    val nodeDestination = ePackage.createEClass("NodeDestination").apply {
        eSuperTypes.add(destination)
    }
    val textFileDestination = ePackage.createEClass("TextFileDestination").apply {
        eSuperTypes.add(destination)
        addContainment("position", position, 0, 1)
    }
    val astNode = ePackage.createEClass("ASTNode").apply {
        this.isAbstract = true
        this.eSuperTypes.add(origin)
        addContainment("position", position, 0, 1)
        addReference("origin", origin, 0, 1)
        addContainment("destination", destination, 0, 1)
    }
    nodeDestination.apply {
        addReference("node", astNode, 1, 1)
    }

    ePackage.createEClass("Statement").apply {
        this.isInterface = true
    }
    ePackage.createEClass("Expression").apply {
        this.isInterface = true
    }
    ePackage.createEClass("EntityDeclaration").apply {
        this.isInterface = true
    }

    val issueType = EcoreFactory.eINSTANCE.createEEnum()
    issueType.name = "IssueType"
    issueType.addLiteral(IssueType.LEXICAL)
    issueType.addLiteral(IssueType.SYNTACTIC)
    issueType.addLiteral(IssueType.SEMANTIC)
    ePackage.eClassifiers.add(issueType)

    val issueSeverity = EcoreFactory.eINSTANCE.createEEnum()
    issueSeverity.name = "IssueSeverity"
    issueSeverity.addLiteral(IssueSeverity.ERROR)
    issueSeverity.addLiteral(IssueSeverity.WARNING)
    issueSeverity.addLiteral(IssueSeverity.INFO)
    ePackage.eClassifiers.add(issueSeverity)

    val issue = ePackage.createEClass("Issue").apply {
        addAttribute("type", issueType, 1, 1)
        addAttribute("message", stringDT, 1, 1)
        addAttribute("severity", issueSeverity, 0, 1)
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
            name = "N"
            eBounds.add(
                EcoreFactory.eINSTANCE.createEGenericType().apply {
                    eClassifier = astNode
                }
            )
        }
        this.eTypeParameters.add(typeParameter)
        val rootContainment = EcoreFactory.eINSTANCE.createEReference()
        rootContainment.name = "referenced"
        rootContainment.eGenericType = EcoreFactory.eINSTANCE.createEGenericType().apply {
            eTypeParameter = typeParameter
        }
        rootContainment.isContainment = false
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
    KOLASU_METAMODEL.saveEcore(File("kolasu-2.0.ecore"))
    KOLASU_METAMODEL.saveEcore(File("kolasu-2.0.xmi"))
    KOLASU_METAMODEL.saveAsJson(File("kolasu-2.0.json"))
}
