package com.strumenta.kolasu.validation

data class Result<C>(
    val issues: List<Issue>,
    val root: C?,
) {
    val lexicalIssues
        get() = issues.filter { it.type == IssueType.LEXICAL }.toList()
    val syntacticIssues
        get() = issues.filter { it.type == IssueType.SYNTACTIC }.toList()
    val semanticIssues
        get() = issues.filter { it.type == IssueType.SEMANTIC }.toList()
    val translationIssues
        get() = issues.filter { it.type == IssueType.TRANSLATION }.toList()
    val errors
        get() = issues.filter { it.severity == IssueSeverity.ERROR }.toList()
    val warnings
        get() = issues.filter { it.severity == IssueSeverity.WARNING }.toList()
    val isCorrect: Boolean
        get() = issues.isEmpty()

    companion object {
        fun <C> exception(
            errorType: IssueType,
            e: Throwable,
        ): Result<C> {
            val errors =
                listOf(
                    Issue(
                        type = errorType,
                        message = e.message ?: e.javaClass.simpleName,
                        range = null,
                    ),
                )
            return Result(errors, null)
        }
    }
}
