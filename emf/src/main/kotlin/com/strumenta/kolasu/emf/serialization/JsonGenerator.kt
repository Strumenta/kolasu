package com.strumenta.kolasu.emf.serialization

import com.strumenta.kolasu.emf.KOLASU_METAMODEL
import com.strumenta.kolasu.emf.getEClass
import com.strumenta.kolasu.emf.saveAsJson
import com.strumenta.kolasu.emf.toEObject
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.Result
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EPackage

class JsonGenerator {
    private fun pointAsEObject(point: Point): EObject {
        val ec = KOLASU_METAMODEL.getEClass(Point::class.java)
        val eo = KOLASU_METAMODEL.eFactoryInstance.create(ec)
        val lineSF = ec.eAllStructuralFeatures.find { it.name == "line" }!!
        eo.eSet(lineSF, point.line)
        val columnSF = ec.eAllStructuralFeatures.find { it.name == "column" }!!
        eo.eSet(columnSF, point.column)
        return eo
    }

    private fun positionAsEObject(position: Position): EObject {
        val ec = KOLASU_METAMODEL.getEClass(Position::class.java)
        val eo = KOLASU_METAMODEL.eFactoryInstance.create(ec)
        val startSF = ec.eAllStructuralFeatures.find { it.name == "start" }!!
        eo.eSet(startSF, pointAsEObject(position.start))
        val stopSF = ec.eAllStructuralFeatures.find { it.name == "end" }!!
        eo.eSet(stopSF, pointAsEObject(position.end))
        return eo
    }

    private fun issueAsEObject(issue: Issue): EObject {
        val ec = KOLASU_METAMODEL.getEClass(Issue::class.java)
        val eo = KOLASU_METAMODEL.eFactoryInstance.create(ec)
        val typeSF = ec.eAllStructuralFeatures.find { it.name == "type" }!!
        // TODO eo.eSet(typeSF, issue.type.ordinal)
        val messageSF = ec.eAllStructuralFeatures.find { it.name == "message" }!!
        eo.eSet(messageSF, issue.message)
        val positionSF = ec.eAllStructuralFeatures.find { it.name == "position" }!!
        if (issue.position != null) {
            eo.eSet(positionSF, positionAsEObject(issue.position!!))
        }
        return eo
    }

    fun generateEMFString(result: Result<out Node>, astPackage: EPackage): String {
        val resultEC = KOLASU_METAMODEL.getEClass(Result::class.java)
        val issueEC = KOLASU_METAMODEL.getEClass(Issue::class.java)
        val resultEO = KOLASU_METAMODEL.eFactoryInstance.create(resultEC)
        val issuesSF = resultEC.eAllStructuralFeatures.find { it.name == "issues" }!!
        val issues = resultEO.eGet(issuesSF) as MutableList<EObject>
        result.errors.forEach {
            issues.add(issueAsEObject(it))
        }
        val rootSF = resultEC.eAllStructuralFeatures.find { it.name == "root" }!!
        if (result.root != null) {
            resultEO.eSet(rootSF, result.root!!.toEObject(astPackage))
        }

        return resultEO.saveAsJson()
    }
}
