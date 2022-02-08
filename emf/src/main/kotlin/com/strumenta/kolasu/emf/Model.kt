package com.strumenta.kolasu.emf

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.strumenta.kolasu.model.*
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.Result
import org.eclipse.emf.common.util.EList
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.*
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl
import org.eclipse.emfcloud.jackson.resource.JsonResourceFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

fun EPackage.getEClass(javaClass: Class<*>): EClass {
    return this.getEClass(javaClass.eClassifierName)
}

fun EPackage.getEClass(klass: KClass<*>): EClass {
    return this.getEClass(klass.eClassifierName)
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
    val ec = ePackage.getEClass(this::class)
    val eo = ePackage.eFactoryInstance.create(ec)
    ec.eAllAttributes.forEach { attr ->
        val prop = this::class.memberProperties.find { it.name == attr.name } as KProperty1<Any, *>?
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

fun <T : Node> Result<T>.toEObject(astPackage: EPackage): EObject {
    val resultEO = makeResultEObject(this)
    val rootSF = resultEO.eClass().eAllStructuralFeatures.find { it.name == "root" }!!
    if (root != null) {
        resultEO.eSet(rootSF, root!!.toEObject(astPackage))
    }
    return resultEO
}

private fun makeResultEObject(result: Result<*>): EObject {
    val resultEC = KOLASU_METAMODEL.getEClass(Result::class.java)
    val resultEO = KOLASU_METAMODEL.eFactoryInstance.create(resultEC)
    val issuesSF = resultEC.eAllStructuralFeatures.find { it.name == "issues" }!!
    val issues = resultEO.eGet(issuesSF) as MutableList<EObject>
    result.issues.forEach {
        issues.add(it.toEObject())
    }
    return resultEO
}

fun <T : Node> Result<T>.toEObject(resource: Resource): EObject {
    val resultEO = makeResultEObject(this)
    val rootSF = resultEO.eClass().eAllStructuralFeatures.find { it.name == "root" }!!
    if (root != null) {
        resultEO.eSet(rootSF, root!!.toEObject(resource))
    }
    return resultEO
}

fun Issue.toEObject(): EObject {
    val ec = KOLASU_METAMODEL.getEClass(Issue::class.java)
    val eo = KOLASU_METAMODEL.eFactoryInstance.create(ec)
    val et = KOLASU_METAMODEL.getEClassifier("IssueType")
    val es = KOLASU_METAMODEL.getEClassifier("IssueSeverity")
    val typeSF = ec.eAllStructuralFeatures.find { it.name == "type" }!!
    eo.eSet(typeSF, (et as EEnum).getEEnumLiteral(type.ordinal))
    val severitySF = ec.eAllStructuralFeatures.find { it.name == "severity" }!!
    eo.eSet(severitySF, (es as EEnum).getEEnumLiteral(severity.ordinal))
    val messageSF = ec.eAllStructuralFeatures.find { it.name == "message" }!!
    eo.eSet(messageSF, message)
    val positionSF = ec.eAllStructuralFeatures.find { it.name == "position" }!!
    if (position != null) {
        eo.eSet(positionSF, position!!.toEObject())
    }
    return eo
}

private fun toValue(ePackage: EPackage, value: Any?): Any? {
    val pdValue: Any? = value
    if (pdValue is Enum<*>) {
        val ee = ePackage.getEEnum(pdValue.javaClass)
        return ee.getEEnumLiteral(pdValue.name)
    } else if (pdValue is LocalDate) {
        return toLocalDateObject(pdValue)
    } else if (pdValue is LocalTime) {
        return toLocalTimeObject(pdValue)
    } else if (pdValue is LocalDateTime) {
        val eClass = KOLASU_METAMODEL.getEClass("LocalDateTime")
        val eObject = KOLASU_METAMODEL.eFactoryInstance.create(eClass)
        val dateComponent = toLocalDateObject(pdValue.toLocalDate())
        val timeComponent = toLocalTimeObject(pdValue.toLocalTime())
        eObject.eSet(eClass.getEStructuralFeature("date"), dateComponent)
        eObject.eSet(eClass.getEStructuralFeature("time"), timeComponent)
        return eObject
    } else {
        // this could be not a primitive value but a value that we mapped to an EClass
        val eClass = if (pdValue != null) {
            ePackage.eClassifiers.filterIsInstance<EClass>().find {
                it.name == pdValue.javaClass.simpleName
            }
        } else null
        return when {
            eClass != null -> {
                pdValue!!.dataToEObject(ePackage)
            }
            pdValue is ReferenceByName<*> -> {
                val refEC = KOLASU_METAMODEL.getEClass("ReferenceByName")
                val refEO = KOLASU_METAMODEL.eFactoryInstance.create(refEC)
                refEO.eSet(refEC.getEStructuralFeature("name")!!, pdValue.name)
                // TODO complete
                refEO
            }
            pdValue is Result<*> -> {
                val resEC = KOLASU_METAMODEL.getEClass("Result")
                val resEO = KOLASU_METAMODEL.eFactoryInstance.create(resEC)
                if (pdValue.root is Node) {
                    resEO.eSet(resEC.getEStructuralFeature("root"), (pdValue.root as Node).toEObject(ePackage))
                } else {
                    resEO.eSet(resEC.getEStructuralFeature("root"), toValue(ePackage, pdValue.root))
                }
                val issues = resEO.eGet(resEC.getEStructuralFeature("issues")) as EList<EObject>
                issues.addAll(pdValue.issues.map { it.toEObject() })
                resEO
            }
            else -> pdValue
        }
    }
}

private fun toLocalTimeObject(value: LocalTime): EObject {
    val eClass = KOLASU_METAMODEL.getEClass("LocalTime")
    val eObject = KOLASU_METAMODEL.eFactoryInstance.create(eClass)
    eObject.eSet(eClass.getEStructuralFeature("hour"), value.hour)
    eObject.eSet(eClass.getEStructuralFeature("minute"), value.minute)
    eObject.eSet(eClass.getEStructuralFeature("second"), value.second)
    eObject.eSet(eClass.getEStructuralFeature("nanosecond"), value.nano)
    return eObject
}

private fun toLocalDateObject(value: LocalDate): EObject {
    val eClass = KOLASU_METAMODEL.getEClass("LocalDate")
    val eObject = KOLASU_METAMODEL.eFactoryInstance.create(eClass)
    eObject.eSet(eClass.getEStructuralFeature("year"), value.year)
    eObject.eSet(eClass.getEStructuralFeature("month"), value.monthValue)
    eObject.eSet(eClass.getEStructuralFeature("dayOfMonth"), value.dayOfMonth)
    return eObject
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

fun Resource.getEClass(klass: KClass<*>): EClass = this.findEClass(klass)
    ?: throw ClassNotFoundException(klass.qualifiedName)

fun Node.toEObject(ePackage: EPackage): EObject = toEObject(ePackage.eResource())

/**
 * Translates this node – and, recursively, its descendants – into an [EObject] (EMF/Ecore representation).
 *
 * The classes of the node are resolved against the provided [Resource]. That is, the resource must contain:
 *  - the [Kolasu metamodel package][KOLASU_METAMODEL]
 *  - every [EPackage] containing the definitions of the node classes in the tree.
 */
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
                    val elist = eo.eGet(esf) as MutableList<EObject?>
                    (pd.value as List<*>?)?.forEach {
                        try {
                            val childEO = (it as Node?)?.toEObject(eResource)
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
                    (pd.value as List<*>?)?.forEach {
                        try {
                            val childValue = toValue(ec.ePackage, it)
                            elist.add(childValue!!)
                        } catch (e: Exception) {
                            throw RuntimeException("Unable to map to EObject child $it in property $pd of $this", e)
                        }
                    }
                } else try {
                    eo.eSet(esf, toValue(ec.ePackage, pd.value))
                } catch (e: Exception) {
                    throw RuntimeException("Unable to set property $pd. Structural feature: $esf", e)
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
