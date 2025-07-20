package com.strumenta.kolasu.codebase

import com.strumenta.kolasu.model.Node

/**
 * A collection of files.
 */
interface Codebase<R : Node> {
    val name: String

    fun files(): Sequence<CodebaseFile<R>>

    fun fileByRelativePath(relativePath: String): CodebaseFile<R>?
}
