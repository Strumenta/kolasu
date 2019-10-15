package com.strumenta.kolasu

import com.strumenta.kolasu.validation.ErrorType

data class Result<C>(val errors: List<com.strumenta.kolasu.validation.Error>, val root: C?) {
    val lexicalErrors
        get() = errors.filter { it.type == ErrorType.LEXICAL }.toList()
    val correct: Boolean
        get() = errors.isEmpty()

    companion object {
        fun <C> exception(errorType: ErrorType, e: Throwable): Result<C> {
            val errors = listOf(
                com.strumenta.kolasu.validation.Error(
                    type = errorType,
                    message = e.message ?: e.javaClass.simpleName,
                    position = null
                )
            )
            return Result(errors, null)
        }
    }
}
