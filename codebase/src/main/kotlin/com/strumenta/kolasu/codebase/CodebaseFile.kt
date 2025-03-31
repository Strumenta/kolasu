package com.strumenta.kolasu.codebase

import com.strumenta.kolasu.model.CodeBaseSource
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.PropertyType
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.traversing.walk
import com.strumenta.kolasu.validation.Issue
import java.io.File

data class CodebaseFile<R : Node>(
    val codebase: Codebase<R>,
    val relativePath: String,
    var code: String,
    var ast: R,
    val parsingIssues: List<Issue> = emptyList()
) {

    init {
        ast.source = this.asSource()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CodebaseFile<*>) return false

        if (relativePath != other.relativePath) return false

        return true
    }

    override fun hashCode(): Int {
        return relativePath.hashCode()
    }

    fun areAllReferencesResolved(): Boolean {
        return ast.walk().all {
            it.originalProperties.filter { it.propertyType == PropertyType.REFERENCE }.all { property ->
                (property.value as ReferenceByName<*>).resolved
            }
        }
    }

    fun hasExtension(extension: String): Boolean {
        return this.relativePath.lowercase().endsWith(".${extension.lowercase()}")
    }

    fun asSource(): CodeBaseSource {
        return CodeBaseSource(codebase.name, relativePath)
    }
}

fun <R : Node> CodebaseFile<R>.hasExtension(extension: String): Boolean {
    return this.relativePath.lowercase().endsWith(".${extension.lowercase()}")
}

fun <R : Node> Codebase<R>.filesWithExtension(vararg extensions: String): Sequence<CodebaseFile<R>> {
    return sequence {
        val it = files().iterator()
        while (it.hasNext()) {
            val codebaseFile = it.next()
            if (extensions.any { extension ->
                codebaseFile.relativePath.lowercase().endsWith(
                        ".${extension.lowercase()}"
                    )
            }
            ) {
                yield(codebaseFile)
            }
        }
    }
}

val <R : Node> CodebaseFile<R>.nameWithoutExtension: String
    get() {
        val simpleName = relativePath.split("/").last()
        return File(simpleName).nameWithoutExtension
    }
