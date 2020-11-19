package com.strumenta.kolasu.model

import kotlinx.serialization.Serializable
import org.antlr.v4.runtime.Token

/**
 * The line should be in 1..n, the column in 0..n
 */
@Serializable
data class Point(val line: Int, val column: Int) : Comparable<Point> {
    override fun compareTo(other: Point): Int {
        if (line == other.line) {
            return this.column - other.column
        }
        return this.line - other.line
    }

    init {
        require(line >= 1) { "Line should be equal or greater than 1, was $line" }
        require(column >= 0) { "Column should be equal or greater than 0, was $column" }
    }

    override fun toString() = "Line $line, Column $column"

    /**
     * Translate the Point to an offset in the original code stream.
     */
    fun offset(code: String): Int {
        val lines = code.split("\n")
        require(lines.size >= line) {
            "The point does not exist in the given text. It indicates line $line but there are only ${lines.size} lines" }
        require(lines[line - 1].length >= column) {
            "The column does not exist in the given text. Line $line has ${lines[line - 1].length} columns, the point indicates column $column" }
        val newLines = this.line - 1
        return lines.subList(0, this.line - 1).foldRight(0, { it, acc -> it.length + acc }) + newLines + column
    }

    fun isBefore(other: Point) = this < other
    fun isSameOrBefore(other: Point) = this <= other

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

val START_POINT = Point(1, 0)

/**
 * Both the start point and the end point are included
 */
@Serializable
data class Position(val start: Point, val end: Point) : Comparable<Position> {

    override fun compareTo(other: Position): Int {
        val cmp = this.start.compareTo(other.start)
        return if (cmp == 0) {
            this.end.compareTo(other.end)
        } else {
            cmp
        }
    }

    init {
        require(start.isBefore(end) || start == end) {
            "End should follows start or be the same as start (start: $start, end: $end)" }
    }

    /**
     * Given the whole code extract the portion of text corresponding to this position
     */
    fun text(wholeText: String): String {
        return wholeText.substring(start.offset(wholeText), end.offset(wholeText))
    }

    fun length(code: String) = end.offset(code) - start.offset(code)

    fun contains(point: Point): Boolean {
        return ((point == start || start.isBefore(point)) && (point == end || point.isBefore(end)))
    }
}

/**
 * Utility function to create a Position
 */
fun pos(startLine: Int, startCol: Int, endLine: Int, endCol: Int) = Position(Point(startLine, startCol), Point(endLine, endCol))

fun Node.isBefore(other: Node): Boolean = position!!.start.isBefore(other.position!!.start)

val Token.startPoint: Point
    get() = Point(this.line, this.charPositionInLine)

val Token.endPoint: Point
    get() = startPoint + this.text
