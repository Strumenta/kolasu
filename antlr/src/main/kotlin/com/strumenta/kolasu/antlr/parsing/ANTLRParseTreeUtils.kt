package com.strumenta.kolasu.antlr.parsing

import com.strumenta.kolasu.model.*
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode

/**
 * Navigate the parse tree performing the specified operations on the nodes, either real nodes or nodes
 * representing errors.
 */
fun ParserRuleContext.processDescendantsAndErrors(
    operationOnParserRuleContext: (ParserRuleContext) -> Unit,
    operationOnError: (ErrorNode) -> Unit,
    includingMe: Boolean = true
) {
    if (includingMe) {
        operationOnParserRuleContext(this)
    }
    if (this.children != null) {
        this.children.filterIsInstance(ParserRuleContext::class.java).forEach {
            it.processDescendantsAndErrors(operationOnParserRuleContext, operationOnError, includingMe = true)
        }
        this.children.filterIsInstance(ErrorNode::class.java).forEach {
            operationOnError(it)
        }
    }
}

/**
 * Get the original text associated to this non-terminal by querying the inputstream.
 */
fun ParserRuleContext.getOriginalText(): String {
    val a: Int = this.start.startIndex
    val b: Int = this.stop.stopIndex
    val interval = org.antlr.v4.runtime.misc.Interval(a, b)
    return this.start.inputStream.getText(interval)
}

/**
 * Get the original text associated to this terminal by querying the inputstream.
 */
fun TerminalNode.getOriginalText(): String = this.symbol.getOriginalText()

/**
 * Get the original text associated to this token by querying the inputstream.
 */
fun Token.getOriginalText(): String {
    val a: Int = this.startIndex
    val b: Int = this.stopIndex
    val interval = org.antlr.v4.runtime.misc.Interval(a, b)
    return this.inputStream.getText(interval)
}

/**
 * Given the entire code, this returns the slice covered by this Node.
 */
fun Node.getText(code: String): String? = range?.text(code)

/**
 * An Origin corresponding to a ParseTreeNode. This is used to indicate that an AST Node has been obtained
 * by mapping an original ParseTreeNode.
 *
 * Note that this is NOT serializable as ParseTree elements are not Serializable.
 */
class ParseTreeOrigin(val parseTree: ParseTree, override var source: Source? = null) : Origin {
    override val range: Range?
        get() = parseTree.toRange(source = source)

    override val sourceText: String?
        get() =
            when (parseTree) {
                is ParserRuleContext -> {
                    parseTree.getOriginalText()
                }
                is TerminalNode -> {
                    parseTree.text
                }
                else -> null
            }
}

/**
 * Set the origin of the AST node as a ParseTreeOrigin, providing the parseTree is not null.
 * If the parseTree is null, no operation is performed.
 */
fun <T : Node> T.withParseTreeNode(parseTree: ParserRuleContext?, source: Source? = null): T {
    if (parseTree != null) {
        this.origin = ParseTreeOrigin(parseTree, source)
    }
    return this
}

val RuleContext.hasChildren: Boolean
    get() = this.childCount > 0

val RuleContext.firstChild: ParseTree?
    get() = if (hasChildren) this.getChild(0) else null

val RuleContext.lastChild: ParseTree?
    get() = if (hasChildren) this.getChild(this.childCount - 1) else null

val Token.length
    get() = if (this.type == Token.EOF) 0 else text.length

val Token.startPoint: Point
    get() = Point(this.line, this.charPositionInLine)

val Token.endPoint: Point
    get() = if (this.type == Token.EOF) startPoint else startPoint + this.text

val Token.range: Range
    get() = Range(startPoint, endPoint)

/**
 * Returns the position of the receiver parser rule context.
 */
val ParserRuleContext.range: Range
    get() = Range(start.startPoint, stop.endPoint)

/**
 * Returns the position of the receiver parser rule context.
 * @param considerPosition if it's false, this method returns null.
 */
fun ParserRuleContext.toRange(considerPosition: Boolean = true, source: Source? = null): Range? {
    return if (considerPosition && start != null && stop != null) {
        val position = range
        if (source == null) position else Range(position.start, position.end, source)
    } else null
}

fun TerminalNode.toRange(considerPosition: Boolean = true, source: Source? = null): Range? =
    this.symbol.toRange(considerPosition, source)

fun Token.toRange(considerPosition: Boolean = true, source: Source? = null): Range? =
    if (considerPosition) Range(this.startPoint, this.endPoint, source) else null

fun ParseTree.toRange(considerRange: Boolean = true, source: Source? = null): Range? {
    return when (this) {
        is TerminalNode -> this.toRange(considerRange, source)
        is ParserRuleContext -> this.toRange(considerRange, source)
        else -> null
    }
}
