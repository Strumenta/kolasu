package com.strumenta.kolasu.validation

data class Result<C>(val issues: List<Issue>, val root: C?) {
    val lexicalErrors
        get() = issues.filter { it.type == IssueType.LEXICAL }.toList()
    val correct: Boolean
        get() = issues.isEmpty()

    companion object {
        fun <C> exception(errorType: IssueType, e: Throwable): Result<C> {
            val errors = listOf(
                Issue(
                    type = errorType,
                    message = e.message ?: e.javaClass.simpleName,
                    position = null
                )
            )
            return Result(errors, null)
        }
    }
}
