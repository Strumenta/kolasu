package me.tomassetti.kolasu.model

data class Point(val line: Int, val column: Int) {
    override fun toString() = "Line $line, Column $column"

    /**
     * Translate the Point to an offset in the original code stream.
     */
    fun offset(code: String) : Int {
        val lines = code.split("\n")
        val newLines = this.line - 1
        return lines.subList(0, this.line - 1).foldRight(0, { it, acc -> it.length + acc }) + newLines + column
    }

    fun isBefore(other: Point) : Boolean = line < other.line || (line == other.line && column < other.column)

}

data class Position(val start: Point, val end: Point) {

    init {
        if (end.isBefore(start)) {
            throw IllegalArgumentException("End should follows start")
        }
    }

    /**
     * Given the whole code extract the portion of text corresponding to this position
     */
    fun text(wholeText: String): String {
        return wholeText.substring(start.offset(wholeText), end.offset(wholeText))
    }

    fun length(code: String) = end.offset(code) - start.offset(code)

    fun contains(point: Point) : Boolean {
        return ((point == start || start.isBefore(point)) && (point == end || point.isBefore(end)))
    }
}

/**
 * Utility function to create a Position
 */
fun pos(startLine:Int, startCol:Int, endLine:Int, endCol:Int) = Position(Point(startLine,startCol),Point(endLine,endCol))

fun Node.isBefore(other: Node) : Boolean = position!!.start.isBefore(other.position!!.start)
