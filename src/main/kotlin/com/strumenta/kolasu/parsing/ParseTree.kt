package com.strumenta.kolasu.parsing

import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.model.endPoint
import com.strumenta.kolasu.model.startPoint
import com.strumenta.kolasu.validation.Error
import com.strumenta.kolasu.validation.ErrorType
import java.util.LinkedList
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.tree.ErrorNode
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
    val children = LinkedList<ParseTreeElement>()
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
        children.forEach { c -> sb.append(c.multiLineString(indentation + "  ")) }
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

fun verifyParseTree(parser: Parser, errors: MutableList<Error>, root: ParserRuleContext) {
    val commonTokenStream = parser.tokenStream as CommonTokenStream
    val lastToken = commonTokenStream.get(commonTokenStream.index())
    if (lastToken.type != Token.EOF) {
        errors.add(Error(ErrorType.SYNTACTIC, "Not whole input consumed", lastToken!!.endPoint.asPosition))
    }

    root.processDescendantsAndErrors(
        {
            if (it.exception != null) {
                errors.add(Error(ErrorType.SYNTACTIC, "Recognition exception: ${it.exception.message}", it.start.startPoint.asPosition))
            }
        },
        {
            errors.add(Error(ErrorType.SYNTACTIC, "Error node found", it.toPosition(true)))
        }
    )
}

fun TerminalNode.toPosition(considerPosition: Boolean = true): Position? {
    return if (considerPosition) {
        Position(this.symbol.startPoint, this.symbol.endPoint)
    } else {
        null
    }
}
