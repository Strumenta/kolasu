package com.strumenta.kolasu.model

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
data class Point(
    val line: Int,
    val column: Int,
) : Comparable<Point> {
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

    fun rangeWithLength(length: Int): Range {
        require(length >= 0)
        return Range(this, this.plus(length))
    }

    /**
     * Translate the Point to an offset in the original code stream.
     */
    fun offset(code: String): Int {
        val lines = code.split("\r\n", "\n", "\r")
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
        if (text.isEmpty()) {
            return this
        }
        var line = this.line
        var column = this.column
        var i = 0
        while (i < text.length) {
            if (text[i] == '\n' || text[i] == '\r') {
                line++
                column = 0
                if (text[i] == '\r' && i < text.length - 1 && text[i + 1] == '\n') {
                    i++ // Count the \r\n sequence as 1 line
                }
            } else {
                column++
            }
            i++
        }
        return Point(line, column)
    }

    val asRange: Range
        get() = Range(this, this)
}

/**
 * An area in a source file, from start to end.
 * The start point is the point right before the starting character.
 * The end point is the point right after the last character.
 * An empty range will have coinciding points.
 *
 * Consider a file with one line, containing text "HELLO".
 * The Range of such text will be Range(Point(1, 0), Point(1, 5)).
 */
data class Range(
    val start: Point,
    val end: Point,
    var source: Source? = null,
) : Comparable<Range> {
    constructor(
        startLine: Int,
        startCol: Int,
        endLine: Int,
        endCol: Int,
    ) : this(
        Point(startLine, startCol),
        Point(endLine, endCol),
    )

    val isFlat: Boolean
        get() = start == end

    override fun toString(): String {
        return "Range(start=$start, end=$end${if (source == null) "" else ", source=$source"})"
    }

    override fun compareTo(other: Range): Int {
        val sourceCmp = compareSources(this.source, other.source)
        if (sourceCmp != 0) {
            return sourceCmp
        }

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
     * Given the whole code extract the portion of text corresponding to this range
     */
    fun text(wholeText: String): String = wholeText.substring(start.offset(wholeText), end.offset(wholeText))

    /**
     * The length in characters of the text under this range in the provided source.
     * @param code the source text.
     */
    fun length(code: String) = end.offset(code) - start.offset(code)

    fun isEmpty(): Boolean = start == end

    /**
     * Tests whether the given point is contained in the interval represented by this object.
     * @param point the point.
     */
    fun contains(point: Point): Boolean =
        ((point == start || start.isBefore(point)) && (point == end || point.isBefore(end)))

    /**
     * Tests whether the given range is contained in the interval represented by this object.
     * @param range the range
     */
    fun contains(range: Range?): Boolean =
        (range != null) &&
            this.start.isSameOrBefore(range.start) &&
            this.end.isSameOrAfter(range.end)

    /**
     * Tests whether the given range overlaps the interval represented by this object.
     * @param range the range
     */
    fun overlaps(range: Range?): Boolean =
        (range != null) && (
            (this.start.isSameOrAfter(range.start) && this.start.isSameOrBefore(range.end)) ||
                (this.end.isSameOrAfter(range.start) && this.end.isSameOrBefore(range.end)) ||
                (range.start.isSameOrAfter(this.start) && range.start.isSameOrBefore(this.end)) ||
                (range.end.isSameOrAfter(this.start) && range.end.isSameOrBefore(this.end))
        )
}

fun compareSources(
    sourceA: Source?,
    sourceB: Source?,
): Int =
    when {
        sourceA == null && sourceB == null -> {
            0
        }

        sourceA == null && sourceB != null -> {
            -1
        }

        sourceA != null && sourceB == null -> {
            1
        }

        else -> {
            sourceA!!.compareTo(sourceB!!)
        }
    }
