package me.tomassetti.kolasu.validation

import me.tomassetti.kolasu.model.Position

enum class ErrorType {
    LEXICAL,
    SYNTACTIC,
    SEMANTIC
}

data class Error(val type: ErrorType, val message: String, val position: Position?)
