package com.strumenta.kolasu.antlr.parsing

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Origin
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Range
import com.strumenta.kolasu.model.Source
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import kotlin.reflect.KClass

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
 *
 * Note that this, differently from Node.parseTreeNode, permits to specify also a Source.
 */
fun <T : Node> T.withParseTreeNode(parseTree: ParseTree?, source: Source? = null): T {
    if (parseTree != null) {
        this.origin = ParseTreeOrigin(parseTree, source)
    }
    return this
}

var Node.parseTreeNode: ParseTree?
    get() {
        return (this.origin as? ParseTreeOrigin)?.parseTree
    }
    set(value) {
        if (value != null) {
            this.origin = ParseTreeOrigin(value)
        }
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
 * Returns the range of the receiver parser rule context.
 */
val ParserRuleContext.range: Range
    get() = Range(start.startPoint, stop.endPoint)

/**
 * Returns the range of the receiver parser rule context.
 * @param considerRange if it's false, this method returns null.
 */
fun ParserRuleContext.toRange(considerRange: Boolean = true, source: Source? = null): Range? {
    return if (considerRange && start != null && stop != null) {
        val range = range
        if (source == null) range else Range(range.start, range.end, source)
    } else {
        null
    }
}

fun TerminalNode.toRange(considerRange: Boolean = true, source: Source? = null): Range? =
    this.symbol.toRange(considerRange, source)

fun Token.toRange(considerRange: Boolean = true, source: Source? = null): Range? =
    if (considerRange) Range(this.startPoint, this.endPoint, source) else null

fun ParseTree.toRange(considerRange: Boolean = true, source: Source? = null): Range? {
    return when (this) {
        is TerminalNode -> this.toRange(considerRange, source)
        is ParserRuleContext -> this.toRange(considerRange, source)
        else -> null
    }
}

/**
 * Find the ancestor of the given element with the given class.
 */
inline fun <reified T : RuleContext> RuleContext.ancestor(): T {
    return this.ancestor(T::class)
}

/**
 * Find the ancestor of the given element with the given class.
 */
fun <T : RuleContext> RuleContext.ancestor(kclass: KClass<T>): T {
    return if (this.parent == null) {
        throw IllegalStateException("Cannot find ancestor of type $kclass")
    } else if (kclass.isInstance(this.parent)) {
        this.parent as T
    } else {
        this.parent.ancestor(kclass)
    }
}
