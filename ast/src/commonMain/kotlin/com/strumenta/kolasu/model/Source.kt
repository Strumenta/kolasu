package com.strumenta.kolasu.model

abstract class Source : Comparable<Source> {
    protected abstract fun stringDescription(): String

    override fun compareTo(other: Source): Int = this.stringDescription().compareTo(other.stringDescription())
}

interface SourceWithID {
    fun sourceID(): String
}

fun lineRange(
    lineNumber: Int,
    lineCode: String,
    source: Source? = null,
): Range {
    require(lineNumber >= 1) { "Line numbers are expected to be equal or greater than 1" }
    return Range(Point(lineNumber, START_COLUMN), Point(lineNumber, lineCode.length), source)
}

class StringSource(
    val code: String? = null,
) : Source() {
    override fun stringDescription(): String {
        val codeSnippet =
            if (code == null) {
                "<NULL>"
            } else if (code.length > 100) {
                code.substring(0, 100)
            } else {
                code
            }
        return "${this::class.simpleName}:$codeSnippet"
    }
}

data class CodeBaseSource(
    val codebaseName: String,
    val relativePath: String,
) : Source() {
    override fun stringDescription(): String =
        "${this::class.simpleName}:codebase $codebaseName, relative path: $relativePath"
}

/**
 * This source is intended to be used for nodes that are "calculated".
 * For example, nodes representing types that are derived by examining the code
 * but cannot be associated to any specific point in the code.
 *
 * @param description this is a description of the source. It is used to describe the process that calculated the node.
 *                    Examples of values could be "type inference".
 */
data class SyntheticSource(
    val description: String,
) : Source() {
    override fun stringDescription(): String = "${this::class.simpleName}:$description"
}
