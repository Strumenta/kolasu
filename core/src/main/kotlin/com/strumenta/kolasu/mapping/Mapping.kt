package com.strumenta.kolasu.mapping

import com.strumenta.kolasu.model.*
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode

interface ParseTreeToAstMapper<in PTN : ParserRuleContext, out ASTN : Node> {
    fun map(parseTreeNode: PTN): ASTN
}

val Token.length
    get() = if (this.type == Token.EOF) 0 else text.length

/**
 * Returns the position of the receiver parser rule context.
 */
val ParserRuleContext.position: Position
    get() = Position(start.startPoint, stop.endPoint)

/**
 * Returns the position of the receiver parser rule context.
 * @param considerPosition if it's false, this method returns null.
 */
fun ParserRuleContext.toPosition(considerPosition: Boolean = true): Position? {
    return if (considerPosition && start != null && stop != null) {
        position
    } else null
}

fun TerminalNode.toPosition(considerPosition: Boolean = true): Position? =
    this.symbol.toPosition(considerPosition)

fun Token.toPosition(considerPosition: Boolean = true): Position? =
    if(considerPosition) Position(this.startPoint, this.endPoint) else null

fun ParseTree.toPosition(considerPosition: Boolean = true): Position? {
    return when (this) {
        is TerminalNode -> this.toPosition(considerPosition)
        is ParserRuleContext -> this.toPosition(considerPosition)
        else -> null
    }
}