package com.strumenta.kolasu.parsing

import com.strumenta.kolasu.model.*
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueType
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode

abstract class ParseTreeElement {
    abstract fun multiLineString(indentation: String = ""): String
}

class ParseTreeLeaf(val type: String, val text: String) : ParseTreeElement() {
    override fun toString(): String {
        return "T:$type[$text]"
    }

    override fun multiLineString(indentation: String): String = "${indentation}T:$type[$text]\n"
}

class ParseTreeNode(val name: String) : ParseTreeElement() {
    val children = mutableListOf<ParseTreeElement>()
    fun child(c: ParseTreeElement): ParseTreeNode {
        children.add(c)
        return this
    }

    override fun toString(): String {
        return "Node($name) $children"
    }

    override fun multiLineString(indentation: String): String {
        val sb = StringBuilder()
        sb.append("${indentation}$name\n")
        children.forEach { c -> sb.append(c.multiLineString("$indentation  ")) }
        return sb.toString()
    }
}

fun toParseTree(node: ParserRuleContext, vocabulary: Vocabulary): ParseTreeNode {
    val res = ParseTreeNode(node.javaClass.simpleName.removeSuffix("Context"))
    node.children?.forEach { c ->
        when (c) {
            is ParserRuleContext -> res.child(toParseTree(c, vocabulary))
            is TerminalNode -> res.child(ParseTreeLeaf(vocabulary.getSymbolicName(c.symbol.type), c.text))
        }
    }
    return res
}

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

@Deprecated("Use KolasuParser")
fun verifyParseTree(parser: Parser, errors: MutableList<Issue>, root: ParserRuleContext) {
    val commonTokenStream = parser.tokenStream as CommonTokenStream
    val lastToken = commonTokenStream.get(commonTokenStream.index())
    if (lastToken.type != Token.EOF) {
        errors.add(Issue(IssueType.SYNTACTIC, "Not whole input consumed", position = lastToken!!.endPoint.asPosition))
    }

    root.processDescendantsAndErrors(
        {
            if (it.exception != null) {
                errors.add(
                    Issue(
                        IssueType.SYNTACTIC,
                        "Recognition exception: ${it.exception.message}",
                        position = it.start.startPoint.asPosition
                    )
                )
            }
        },
        {
            errors.add(
                Issue(
                    IssueType.SYNTACTIC, "Error node found (token: ${it.symbol?.text})",
                    position = it.toPosition(true)
                )
            )
        }
    )
}

fun ParserRuleContext.getOriginalText(): String {
    val a: Int = this.start.startIndex
    val b: Int = this.stop.stopIndex
    val interval = org.antlr.v4.runtime.misc.Interval(a, b)
    return this.start.inputStream.getText(interval)
}

fun TerminalNode.getOriginalText(): String = this.symbol.getOriginalText()

fun Token.getOriginalText(): String {
    val a: Int = this.startIndex
    val b: Int = this.stopIndex
    val interval = org.antlr.v4.runtime.misc.Interval(a, b)
    return this.inputStream.getText(interval)
}

fun Node.getText(code: String): String? = position?.text(code)

class ParseTreeOrigin(val parseTree: ParseTree, override var source: Source? = null) : Origin {
    override val position: Position?
        get() = parseTree.toPosition(source = source)

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

fun <T : Node> T.withParseTreeNode(tree: ParserRuleContext?): T {
    if (tree != null) {
        this.origin = ParseTreeOrigin(tree)
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

val Token.position: Position
    get() = Position(startPoint, endPoint)

/**
 * Returns the position of the receiver parser rule context.
 */
val ParserRuleContext.position: Position
    get() = Position(start.startPoint, stop.endPoint)

/**
 * Returns the position of the receiver parser rule context.
 * @param considerPosition if it's false, this method returns null.
 */
fun ParserRuleContext.toPosition(considerPosition: Boolean = true, source: Source? = null): Position? {
    return if (considerPosition && start != null && stop != null) {
        val position = position
        if (source == null) position else Position(position.start, position.end, source)
    } else null
}

fun TerminalNode.toPosition(considerPosition: Boolean = true, source: Source? = null): Position? =
    this.symbol.toPosition(considerPosition, source)

fun Token.toPosition(considerPosition: Boolean = true, source: Source? = null): Position? =
    if (considerPosition) Position(this.startPoint, this.endPoint, source) else null

fun ParseTree.toPosition(considerPosition: Boolean = true, source: Source? = null): Position? {
    return when (this) {
        is TerminalNode -> this.toPosition(considerPosition, source)
        is ParserRuleContext -> this.toPosition(considerPosition, source)
        else -> null
    }
}
