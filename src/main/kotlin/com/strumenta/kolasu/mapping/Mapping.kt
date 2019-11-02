package com.strumenta.kolasu.mapping

import com.strumenta.kolasu.mapping.endPoint
import com.strumenta.kolasu.model.*
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token

interface ParseTreeToAstMapper<in PTN : ParserRuleContext, out ASTN : Node> {
    fun map(parseTreeNode: PTN): ASTN
}

fun Token.startPoint() = Point(line, charPositionInLine)

fun Token.endPoint() = Point(line, charPositionInLine + text.length)

val ParserRuleContext.position: Position
    get() = Position(start.startPoint, stop.endPoint)

fun ParserRuleContext.toPosition(considerPosition: Boolean = true): Position? {
    return if (considerPosition && start != null && stop != null) {
        position
    } else null
}
