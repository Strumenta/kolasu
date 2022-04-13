package com.strumenta.kolasu.model

import org.antlr.v4.runtime.RuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.ParseTree
import java.io.File
import java.net.URL
import java.nio.file.Path

val START_LINE = 1
val START_COLUMN = 0
val START_POINT = Point(START_LINE, START_COLUMN)

/**
 * A location in a source code file.
 * The line should be in 1..n, the column in 0..n.
 *
 * Consider a file with one line, containing text "HELLO":
 * - the point before the first character will be Point(1, 0)
 * - the point at the end of the first line, after the letter "O" will be Point(1, 5)
 */
data class Point(val line: Int, val column: Int) : Comparable<Point> {
    override fun compareTo(other: Point): Int {
        if (line == other.line) {
            return this.column - other.column
        }
        return this.line - other.line
    }

    init {
        checkLine(line)
        checkColumn(column)
    }

    companion object {
        fun checkLine(line: Int) {
            require(line >= START_LINE) { "Line should be equal or greater than 1, was $line" }
        }
        fun checkColumn(column: Int) {
            require(column >= START_COLUMN) { "Column should be equal or greater than 0, was $column" }
        }
    }

    override fun toString() = "Line $line, Column $column"

    fun positionWithLength(length: Int): Position {
        require(length >= 0)
        return Position(this, this.plus(length))
    }

    /**
     * Translate the Point to an offset in the original code stream.
     */
    fun offset(code: String, lineEnding: String = System.lineSeparator()): Int {
        val lines = code.split(lineEnding)
        require(lines.size >= line) {
            "The point does not exist in the given text. It indicates line $line but there are only ${lines.size} lines"
        }
        require(lines[line - 1].length >= column) {
            "The column does not exist in the given text. Line $line has ${lines[line - 1].length} columns, " +
                "the point indicates column $column"
        }
        val newLines = this.line - 1
        return lines.subList(0, this.line - 1).foldRight(0) { it, acc -> it.length + acc } + newLines + column
    }

    /**
     * Computes whether this point comes strictly before another point.
     * @param other the other point
     */
    fun isBefore(other: Point) = this < other

    /**
     * Computes whether this point is the same as, or comes before, another point.
     * @param other the other point
     */
    fun isSameOrBefore(other: Point) = this <= other

    /**
     * Computes whether this point is the same as, or comes after, another point.
     * @param other the other point
     */
    fun isSameOrAfter(other: Point) = this >= other

    operator fun plus(length: Int): Point {
        return Point(this.line, this.column + length)
    }

    operator fun plus(text: String): Point {
        return when {
            text.isEmpty() -> this
            text.startsWith("\r\n") -> Point(line + 1, 0) + text.substring(2)
            text.startsWith("\n") || text.startsWith("\r") -> Point(line + 1, 0) + text.substring(1)
            else -> Point(line, column + 1) + text.substring(1)
        }
    }

    val asPosition: Position
        get() = Position(this, this)
}

fun linePosition(lineNumber: Int, lineCode: String, source: Source? = null): Position {
    require(lineNumber >= 1) { "Line numbers are expected to be equal or greater than 1" }
    return Position(Point(lineNumber, START_COLUMN), Point(lineNumber, lineCode.length), source)
}

abstract class Source
class SourceSet(val name: String, val root: Path)
class SourceSetElement(val sourceSet: SourceSet, val relativePath: Path) : Source()
class FileSource(val file: File) : Source()
class StringSource(val code: String? = null) : Source()
class URLSource(val url: URL) : Source()

/**
 * An area in a source file, from start to end.
 * The start point is the point right before the starting character.
 * The end point is the point right after the last character.
 * An empty position will have coinciding points.
 *
 * Consider a file with one line, containing text "HELLO".
 * The Position of such text will be Position(Point(1, 0), Point(1, 5)).
 */
data class Position(val start: Point, val end: Point, var source: Source? = null) : Comparable<Position> {

    override fun toString(): String {
        return "Position(start=$start, end=$end${if (source == null) "" else ", source=$source"})"
    }

    override fun compareTo(other: Position): Int {
        val cmp = this.start.compareTo(other.start)
        return if (cmp == 0) {
            this.end.compareTo(other.end)
        } else {
            cmp
        }
    }

    constructor(start: Point, end: Point, source: Source? = null, validate: Boolean = true) : this(start, end, source) {
        if (validate) {
            require(start.isBefore(end) || start == end) {
                "End should follows start or be the same as start (start: $start, end: $end)"
            }
        }
    }

    /**
     * Given the whole code extract the portion of text corresponding to this position
     */
    fun text(wholeText: String): String {
        return wholeText.substring(start.offset(wholeText), end.offset(wholeText))
    }

    /**
     * The length in characters of the text under this position in the provided source.
     * @param code the source text.
     */
    fun length(code: String) = end.offset(code) - start.offset(code)

    fun isEmpty(): Boolean = start == end

    /**
     * Tests whether the given point is contained in the interval represented by this object.
     * @param point the point.
     */
    fun contains(point: Point): Boolean {
        return ((point == start || start.isBefore(point)) && (point == end || point.isBefore(end)))
    }

    /**
     * Tests whether the given position is contained in the interval represented by this object.
     * @param position the position
     */
    fun contains(position: Position?): Boolean {
        return (position != null) &&
            this.start.isSameOrBefore(position.start) &&
            this.end.isSameOrAfter(position.end)
    }

    /**
     * Tests whether the given node is contained in the interval represented by this object.
     * @param node the node
     */
    fun contains(node: Node): Boolean {
        return this.contains(node.position)
    }
}

/**
 * Utility function to create a Position
 */
fun pos(startLine: Int, startCol: Int, endLine: Int, endCol: Int) = Position(
    Point(startLine, startCol),
    Point(endLine, endCol)
)

fun Node.isBefore(other: Node): Boolean = position!!.start.isBefore(other.position!!.start)

val Node.startLine: Int?
    get() = this.position?.start?.line

val Node.endLine: Int?
    get() = this.position?.end?.line

val Token.length
    get() = if (this.type == Token.EOF) 0 else text.length

val Token.startPoint: Point
    get() = Point(this.line, this.charPositionInLine)

val Token.endPoint: Point
    get() = if (this.type == Token.EOF) startPoint else startPoint + this.text

val RuleContext.hasChildren: Boolean
    get() = this.childCount > 0

val RuleContext.firstChild: ParseTree?
    get() = if (hasChildren) this.getChild(0) else null

val RuleContext.lastChild: ParseTree?
    get() = if (hasChildren) this.getChild(this.childCount - 1) else null
