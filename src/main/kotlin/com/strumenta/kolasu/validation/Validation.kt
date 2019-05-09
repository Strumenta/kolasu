package com.strumenta.kolasu.validation

import com.strumenta.kolasu.model.Position

enum class ErrorType {
    LEXICAL,
    SYNTACTIC,
    SEMANTIC
}

data class Error(val type: ErrorType, val message: String, val position: Position? = null)
