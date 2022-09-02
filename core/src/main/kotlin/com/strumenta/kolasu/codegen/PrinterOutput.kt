package com.strumenta.kolasu.codegen

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.model.START_POINT
import kotlin.reflect.KClass

/**
 * Know how to print a single node type.
 */
fun interface NodePrinter {
    fun print(output: PrinterOutput, ast: Node)
}

/**
 * This provides a mechanism to generate code tracking indentation, handling lists, and providing other facilities.
 * This is used in the implementation of NodePrinter and in ASTCodeGenerator.
 */
class PrinterOutput(private val nodePrinters: Map<KClass<*>, NodePrinter>) {
    private val sb = StringBuilder()
    private var currentPoint = START_POINT
    private var indentationLevel = 0
    private var onNewLine = true
    private var indentationBlock = "    "
    private var newLineStr = "\n"

    fun text() = sb.toString()

    fun println() {
        println("")
    }

    fun println(text: String = "") {
        print(text)
        sb.append(newLineStr)
        currentPoint += newLineStr
        onNewLine = true
    }

    fun printFlag(flag: Boolean, text: String) {
        if (flag) {
            print(text)
        }
    }

    fun print(text: String) {
        if (text == "\n") {
            println()
            return
        }
        considerIndentation()
        require(text.lines().size < 2) { "Given text span multiple lines: $text" }
        sb.append(text)
        currentPoint += text
    }

    private fun considerIndentation() {
        if (onNewLine) {
            onNewLine = false
            (1..(indentationLevel)).forEach {
                print(indentationBlock)
            }
        }
    }

    fun print(text: String?, prefix: String = "", postfix: String = "") {
        if (text == null) {
            return
        }
        print(prefix)
        print(text)
        print(postfix)
    }

    fun print(ast: Node?, prefix: String = "", postfix: String = "") {
        if (ast == null) {
            return
        }
        print(prefix)
        val printer = nodePrinters[ast::class] ?: throw java.lang.IllegalArgumentException("Unable to print $ast")
        associate(ast) {
            printer.print(this, ast)
        }
        print(postfix)
    }

    fun println(ast: Node?, prefix: String = "", postfix: String = "") {
        print(ast, prefix, postfix + "\n")
    }

    fun indent() {
        indentationLevel++
    }
    fun dedent() {
        indentationLevel--
    }
    fun associate(ast: Node, generation: PrinterOutput.() -> Unit) {
        val startPoint = currentPoint
        generation()
        val endPoint = currentPoint
        val nodePositionInGeneratedCode = Position(startPoint, endPoint)
        ast.destination = nodePositionInGeneratedCode
    }

    fun <T : Node> printList(elements: List<T>, separator: String = ", ") {
        var i = 0
        while (i < elements.size) {
            if (i != 0) {
                print(separator)
            }
            print(elements[i])
            i += 1
        }
    }

    fun <T : Node> printList(
        prefix: String,
        elements: List<T>,
        postfix: String,
        printEvenIfEmpty: Boolean = false,
        separator: String = ", "
    ) {
        if (elements.isNotEmpty() || printEvenIfEmpty) {
            print(prefix)
            printList(elements, separator)
            print(postfix)
        }
    }
}
