package com.strumenta.kolasu.playground

import com.strumenta.kolasu.emf.KolasuToEMFMapping
import com.strumenta.kolasu.emf.STARLASU_METAMODEL
import com.strumenta.kolasu.emf.addAttribute
import com.strumenta.kolasu.emf.addContainment
import com.strumenta.kolasu.emf.createEClass
import com.strumenta.kolasu.emf.createResourceSet
import com.strumenta.kolasu.emf.getEClass
import com.strumenta.kolasu.emf.setMultipleContainment
import com.strumenta.kolasu.emf.setResourceURI
import com.strumenta.kolasu.emf.setSingleContainment
import com.strumenta.kolasu.emf.setStringAttribute
import com.strumenta.kolasu.emf.toEObject
import com.strumenta.kolasu.model.ASTNode
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.Result
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.emf.ecore.EcorePackage
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emfcloud.jackson.resource.JsonResourceFactory
import java.io.ByteArrayOutputStream

val TRANSPILATION_METAMODEL by lazy { createTranspilationMetamodel() }

private fun createTranspilationMetamodel(): EPackage {
    val ePackage = EcoreFactory.eINSTANCE.createEPackage()
    val nsUri = "https://strumenta.com/starlasu/transpilation/v1"
    ePackage.setResourceURI(nsUri)
    ePackage.name = "StrumentaLanguageSupportTranspilation"
    ePackage.nsURI = nsUri

    val result = STARLASU_METAMODEL.getEClass("Result")
    val issue = STARLASU_METAMODEL.getEClass("Issue")
    val string = EcorePackage.eINSTANCE.eString

    ePackage.createEClass("TranspilationTrace").apply {
        addAttribute("originalCode", string, 1, 1)
        addContainment("sourceResult", result, 1, 1)
        addContainment("targetResult", result, 1, 1)
        addAttribute("generatedCode", string, 1, 1)
        addContainment("issues", issue, 0, -1)
    }

    return ePackage
}

/**
 * A transpilation trace can be visualized to demonstrate how the transpiler work.
 */
class TranspilationTrace<S : ASTNode, T : ASTNode>(
    val originalCode: String,
    val generatedCode: String,
    val sourceResult: Result<S>,
    val targetResult: Result<T>,
    val transpilationIssues: List<Issue> = emptyList()
) {
    constructor(
        originalCode: String,
        generatedCode: String,
        sourceAST: S,
        targetAST: T,
        transpilationIssues: List<Issue> = emptyList()
    ) : this(
        originalCode,
        generatedCode,
        Result(emptyList(), sourceAST),
        Result(emptyList(), targetAST),
        transpilationIssues
    )
}

private fun <S : ASTNode, T : ASTNode> makeTranspilationTraceEObject(transpilationTrace: TranspilationTrace<S, T>):
    EObject {
    val transpilationTraceEC = TRANSPILATION_METAMODEL.getEClass(TranspilationTrace::class.java)
    val transpilationTraceEO = TRANSPILATION_METAMODEL.eFactoryInstance.create(transpilationTraceEC)
    return transpilationTraceEO
}

fun <S : ASTNode, T : ASTNode> TranspilationTrace<S, T>.toEObject(resource: Resource): EObject {
    val transpilationTraceEO = makeTranspilationTraceEObject(this)
    transpilationTraceEO.setStringAttribute("originalCode", this.originalCode)
    val mapping = KolasuToEMFMapping()
    transpilationTraceEO.setSingleContainment("sourceResult", this.sourceResult.toEObject(resource, mapping))
    transpilationTraceEO.setSingleContainment("targetResult", this.targetResult.toEObject(resource, mapping))
    transpilationTraceEO.setStringAttribute("generatedCode", this.generatedCode)
    transpilationTraceEO.setMultipleContainment("issues", this.transpilationIssues.map { it.toEObject() })
    return transpilationTraceEO
}

fun <S : ASTNode, T : ASTNode> TranspilationTrace<S, T>.saveAsJson(name: String, vararg ePackages: EPackage): String {
    val resourceSet = createResourceSet()
    val resource = resourceSet.createResource(URI.createURI(name))
    ePackages.forEach {
        val packageResource = JsonResourceFactory().createResource(URI.createURI(it.nsURI))
        resourceSet.resources.add(packageResource)
        packageResource.contents.add(it)
    }
    resource.contents.add(this.toEObject(resource))

    val output = ByteArrayOutputStream()
    resource.save(output, null)
    return output.toString(Charsets.UTF_8.name())
}
