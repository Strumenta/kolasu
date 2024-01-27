package com.strumenta.kolasu.playground

import com.strumenta.kolasu.ast.NodeLike
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.Result

/**
 * A transpilation trace can be visualized to demonstrate how the transpiler work.
 * This represents a single file being transpiled into a singl file.
 */
class TranspilationTrace<S : NodeLike, T : NodeLike>(
    val originalCode: String,
    val generatedCode: String,
    val sourceResult: Result<S>,
    val targetResult: Result<T>,
    val transpilationIssues: List<Issue> = emptyList(),
) {
    constructor(
        originalCode: String,
        generatedCode: String,
        sourceAST: S,
        targetAST: T,
        transpilationIssues: List<Issue> = emptyList(),
    ) : this(
        originalCode,
        generatedCode,
        Result(emptyList(), sourceAST),
        Result(emptyList(), targetAST),
        transpilationIssues,
    )
}
