package com.strumenta.kolasu.playground

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.Result

/**
 * A transpilation trace can be visualized to demonstrate how the transpiler work.
 * This transpilation trace represent a set of files being transformed in a second set of files.
 */
class WorkspaceTranspilationTrace {
    data class WorkspaceFile(val path: String, val code: String, val result: Result<Node>)

    val originalFiles: MutableList<WorkspaceFile> = mutableListOf()
    val generatedFiles: MutableList<WorkspaceFile> = mutableListOf()
    val transpilationIssues: MutableList<Issue> = mutableListOf()
}
