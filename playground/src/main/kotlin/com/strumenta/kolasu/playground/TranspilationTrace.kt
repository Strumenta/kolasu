package com.strumenta.kolasu.playground

import com.strumenta.kolasu.emf.KolasuToEMFMapping
import com.strumenta.kolasu.emf.createResourceSet
import com.strumenta.kolasu.emf.getEClass
import com.strumenta.kolasu.emf.setMultipleContainment
import com.strumenta.kolasu.emf.setSingleContainment
import com.strumenta.kolasu.emf.setStringAttribute
import com.strumenta.kolasu.emf.toEObject
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.Result
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emfcloud.jackson.resource.JsonResourceFactory
import java.io.ByteArrayOutputStream

/**
 * A transpilation trace can be visualized to demonstrate how the transpiler work.
 * This represents a single file being transpiled into a singl file.
 */
class TranspilationTrace<S : Node, T : Node>(
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
        originalCode, generatedCode,
        Result(emptyList(), sourceAST), Result(emptyList(), targetAST), transpilationIssues
    )
}

