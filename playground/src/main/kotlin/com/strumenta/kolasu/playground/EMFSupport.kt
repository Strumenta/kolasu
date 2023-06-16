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
import com.strumenta.kolasu.model.Node
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
    val nsUri = "https://strumenta.com/starlasu/transpilation/v2"
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

    val workspaceFile = ePackage.createEClass("WorkspaceFile").apply {
        addAttribute("path", string, 1, 1)
        addAttribute("code", string, 1, 1)
        addContainment("result", result, 1, 1)
    }

    ePackage.createEClass("WorkspaceTranspilationTrace").apply {
        addContainment("originalFiles", workspaceFile, 1, -1)
        addContainment("generatedFiles", workspaceFile, 1, -1)
        addContainment("issues", issue, 0, -1)
    }

    return ePackage
}

private fun makeTranspilationTraceEObject(): EObject {
    val ec = TRANSPILATION_METAMODEL.getEClass(TranspilationTrace::class.java)
    return TRANSPILATION_METAMODEL.eFactoryInstance.create(ec)
}

private fun makeWorkspaceTranspilationTraceEObject(): EObject {
    val ec = TRANSPILATION_METAMODEL.getEClass(WorkspaceTranspilationTrace::class.java)
    return TRANSPILATION_METAMODEL.eFactoryInstance.create(ec)
}

private fun makeWorkspaceFileEObject() : EObject {
    val ec = TRANSPILATION_METAMODEL.getEClass("WorkspaceFile")
    return TRANSPILATION_METAMODEL.eFactoryInstance.create(ec)
}

fun <S : Node, T : Node> TranspilationTrace<S, T>.toEObject(resource: Resource): EObject {
    val transpilationTraceEO = makeTranspilationTraceEObject()
    transpilationTraceEO.setStringAttribute("originalCode", this.originalCode)
    val mapping = KolasuToEMFMapping()
    transpilationTraceEO.setSingleContainment("sourceResult", this.sourceResult.toEObject(resource, mapping))
    transpilationTraceEO.setSingleContainment("targetResult", this.targetResult.toEObject(resource, mapping))
    transpilationTraceEO.setStringAttribute("generatedCode", this.generatedCode)
    transpilationTraceEO.setMultipleContainment("issues", this.transpilationIssues.map { it.toEObject() })
    return transpilationTraceEO
}

fun WorkspaceTranspilationTrace.WorkspaceFile.toEObject(resource: Resource, mapping: KolasuToEMFMapping): EObject {
    val workspaceFileEO = makeWorkspaceFileEObject()
    workspaceFileEO.setStringAttribute("path", this.path)
    workspaceFileEO.setStringAttribute("code", this.code)
    workspaceFileEO.setSingleContainment("result", this.result.toEObject(resource, mapping))
    return workspaceFileEO
}

fun WorkspaceTranspilationTrace.toEObject(resource: Resource): EObject {
    val transpilationTraceEO = makeWorkspaceTranspilationTraceEObject()
    val mapping = KolasuToEMFMapping()

    transpilationTraceEO.setMultipleContainment("originalFiles", this.originalFiles.map { it.toEObject(resource, mapping) })
    transpilationTraceEO.setMultipleContainment("generatedFiles", this.generatedFiles.map { it.toEObject(resource, mapping) })

    transpilationTraceEO.setMultipleContainment("issues", this.transpilationIssues.map { it.toEObject() })
    return transpilationTraceEO
}

fun <S : Node, T : Node> TranspilationTrace<S, T>.saveAsJson(name: String, vararg ePackages: EPackage): String {
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

fun WorkspaceTranspilationTrace.saveAsJson(name: String, vararg ePackages: EPackage): String {
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
