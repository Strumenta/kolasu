package com.strumenta.kolasu.parserbench

import com.strumenta.kolasu.emf.*
import com.strumenta.kolasu.model.Node
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emfcloud.jackson.resource.JsonResourceFactory
import java.io.ByteArrayOutputStream

val TRANSPILATION_METAMODEL by lazy { createTranspilationMetamodel() }

private fun createTranspilationMetamodel(): EPackage {
    val ePackage = EcoreFactory.eINSTANCE.createEPackage()
    val nsUri = "https://strumenta.com/kolasu/transpilation/v1"
    ePackage.setResourceURI(nsUri)
    ePackage.name = "StrumentaLanguageSupportTranspilation"
    ePackage.nsURI = nsUri

    val astNode = KOLASU_METAMODEL.getEClass("ASTNode")
    val string = KOLASU_METAMODEL.getEDataType("string")

    val transpilationTrace = ePackage.createEClass("TranspilationTrace").apply {
        addAttribute("originalCode", string, 1, 1)
        addContainment("sourceAST", astNode, 1, 1)
        addContainment("targetAST", astNode, 1, 1)
        addAttribute("generatedCode", string, 1, 1)
    }

    return ePackage
}

/**
 * A transpilation trace can be visualized to demonstrate how the transpiler work.
 */
class TranspilationTrace<S : Node, T : Node>(
    val originalCode: String,
    val sourceAST: S,
    val targetAST: T,
    val generatedCode: String
)

private fun <S : Node, T : Node> makeTranspilationTraceEObject(transpilationTrace: TranspilationTrace<S, T>): EObject {
    val transpilationTraceEC = TRANSPILATION_METAMODEL.getEClass(TranspilationTrace::class.java)
    val transpilationTraceEO = TRANSPILATION_METAMODEL.eFactoryInstance.create(transpilationTraceEC)
    return transpilationTraceEO
}

fun <S : Node, T : Node> TranspilationTrace<S, T>.toEObject(resource: Resource): EObject {
    val transpilationTraceEO = makeTranspilationTraceEObject(this)
    transpilationTraceEO.setStringAttribute("originalCode", this.originalCode)
    val mapping = KolasuToEMFMapping()
    transpilationTraceEO.setSingleContainment("sourceAST", this.sourceAST.toEObject(resource, mapping))
    transpilationTraceEO.setSingleContainment("targetAST", this.targetAST.toEObject(resource, mapping))
    transpilationTraceEO.setStringAttribute("generatedCode", this.generatedCode)
    return transpilationTraceEO
}

fun <S : Node, T : Node> TranspilationTrace<S, T>.saveAsJson(name: String, vararg ePackages: EPackage): String {
    // val uri: URI = URI.createURI("dummy-URI")
    val resourceSet = ResourceSetImpl()
    resourceSet.resourceFactoryRegistry.extensionToFactoryMap["json"] = JsonResourceFactory()
    val resource = resourceSet.createResource(URI.createURI(name))
    ePackages.forEach {
        val packageResource = JsonResourceFactory().createResource(URI.createURI(it.nsURI))
        resourceSet.resources.add(packageResource)
        packageResource.contents.add(it)
    }
    // val resource: Resource = JsonResourceFactory().createResource(uri)
    // resource.resourceSet = resourceSet
//    ePackages.forEach {
//        resource.contents.add(it)
//    }
    resource.contents.add(this.toEObject(resource))
    val output = ByteArrayOutputStream()
    resource.save(output, null)
    return output.toString(Charsets.UTF_8.name())
}
