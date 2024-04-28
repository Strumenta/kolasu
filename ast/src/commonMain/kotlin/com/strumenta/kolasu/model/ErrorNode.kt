package com.strumenta.kolasu.model

/**
 * An AST node that marks the presence of an error, for example a syntactic or semantic error in the original tree.
 */
interface ErrorNode : NodeLike {
    val message: String
}
