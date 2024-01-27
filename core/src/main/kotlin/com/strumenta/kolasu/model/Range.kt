package com.strumenta.kolasu.model

import com.strumenta.kolasu.ast.Point
import com.strumenta.kolasu.ast.Range
import com.strumenta.kolasu.ast.START_COLUMN
import com.strumenta.kolasu.ast.Source
import java.io.File
import java.net.URL
import java.nio.file.Path

fun lineRange(
    lineNumber: Int,
    lineCode: String,
    source: Source? = null,
): Range {
    require(lineNumber >= 1) { "Line numbers are expected to be equal or greater than 1" }
    return Range(Point(lineNumber, START_COLUMN), Point(lineNumber, lineCode.length), source)
}

class SourceSet(
    val name: String,
    val root: Path,
)

class SourceSetElement(
    val sourceSet: SourceSet,
    val relativePath: Path,
) : Source() {
    override fun stringDescription(): String = "${this.javaClass.name}:$relativePath"
}

data class FileSource(
    val file: File,
) : Source() {
    override fun stringDescription(): String = "${this.javaClass.name}:${file.path}"
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
        return "${this.javaClass.name}:$codeSnippet"
    }
}

class URLSource(
    val url: URL,
) : Source() {
    override fun stringDescription(): String = "${this.javaClass.name}:$url"
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
    override fun stringDescription(): String = "${this.javaClass.name}:$description"
}

/**
 * Tests whether the given node is contained in the interval represented by this object.
 * @param node the node
 */
fun Range.contains(node: NodeLike): Boolean = this.contains(node.range)

/**
 * Utility function to create a Range
 */
fun range(
    startLine: Int,
    startCol: Int,
    endLine: Int,
    endCol: Int,
) = Range(
    Point(startLine, startCol),
    Point(endLine, endCol),
)

fun NodeLike.isBefore(other: NodeLike): Boolean = range!!.start.isBefore(other.range!!.start)

val NodeLike.startLine: Int?
    get() = this.range?.start?.line

val NodeLike.endLine: Int?
    get() = this.range?.end?.line
