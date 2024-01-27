package com.strumenta.kolasu.codegen

import com.strumenta.kolasu.ast.NodeLike
import com.strumenta.kolasu.ast.Range
import com.strumenta.kolasu.ast.START_POINT
import com.strumenta.kolasu.model.TextFileDestination
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

/**
 * Know how to print a single node type.
 */
fun interface NodePrinter {
    fun print(
        output: PrinterOutput,
        ast: NodeLike,
    )
}

/**
 * This provides a mechanism to generate code tracking indentation, handling lists, and providing other facilities.
 * This is used in the implementation of NodePrinter and in ASTCodeGenerator.
 */
class PrinterOutput(
    private val nodePrinters: Map<KClass<*>, NodePrinter>,
    private var nodePrinterOverrider: (node: NodeLike) -> NodePrinter? = { _ -> null },
) {
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

    fun printFlag(
        flag: Boolean,
        text: String,
    ) {
        if (flag) {
            print(text)
        }
    }

    fun print(
        text: String,
        allowMultiLine: Boolean = false,
    ) {
        if (text.isEmpty()) {
            return
        }
        var adaptedText = text
        val needPrintln = adaptedText.endsWith("\n")
        if (needPrintln) {
            adaptedText = adaptedText.removeSuffix("\n")
        }
        considerIndentation()
        require(adaptedText.lines().size < 2 || allowMultiLine) { "Given text span multiple lines: $adaptedText" }
        sb.append(adaptedText)
        currentPoint += adaptedText
        if (needPrintln) {
            println()
        }
    }

    fun print(text: Char) {
        this.print(text.toString())
    }

    fun print(value: Int) {
        this.print(value.toString())
    }

    private fun considerIndentation() {
        if (onNewLine) {
            onNewLine = false
            (1..(indentationLevel)).forEach {
                print(indentationBlock)
            }
        }
    }

    fun print(
        text: String?,
        prefix: String = "",
        postfix: String = "",
    ) {
        if (text == null) {
            return
        }
        print(prefix)
        print(text)
        print(postfix)
    }

    private fun findPrinter(
        ast: NodeLike,
        kclass: KClass<*>,
    ): NodePrinter? {
        val overrider = nodePrinterOverrider(ast)
        if (overrider != null) {
            return overrider
        }
        val properPrinter = nodePrinters[kclass]
        if (properPrinter != null) {
            return properPrinter
        }
        val superclass = kclass.superclasses.filter { !it.java.isInterface }.firstOrNull()
        if (superclass != null) {
            return getPrinter(ast, superclass)
        }
        return null
    }

    private fun getPrinter(
        ast: NodeLike,
        kclass: KClass<*> = ast::class,
    ): NodePrinter {
        val printer = findPrinter(ast, kclass)
        return printer ?: throw java.lang.IllegalArgumentException("Unable to print $ast")
    }

    fun print(
        ast: NodeLike?,
        prefix: String = "",
        postfix: String = "",
    ) {
        if (ast == null) {
            return
        }
        print(prefix)
        val printer = getPrinter(ast)
        associate(ast) {
            printer.print(this, ast)
        }
        print(postfix)
    }

    fun println(
        ast: NodeLike?,
        prefix: String = "",
        postfix: String = "",
    ) {
        print(ast, prefix, postfix + "\n")
    }

    fun printEmptyLine() {
        println()
        println()
    }

    fun indent() {
        indentationLevel++
    }

    fun dedent() {
        indentationLevel--
    }

    fun associate(
        ast: NodeLike,
        generation: PrinterOutput.() -> Unit,
    ) {
        val startPoint = currentPoint
        generation()
        val endPoint = currentPoint
        val nodeRangeInGeneratedCode = Range(startPoint, endPoint)
        ast.destinations += TextFileDestination(range = nodeRangeInGeneratedCode)
    }

    fun <T : NodeLike> printList(
        elements: List<T>,
        separator: String = ", ",
        elementPrinter: (T) -> Unit,
    ) {
        var i = 0
        while (i < elements.size) {
            if (i != 0) {
                print(separator)
            }
            elementPrinter(elements[i])
            i += 1
        }
    }

    fun <T : NodeLike> printList(
        elements: List<T>,
        separator: String = ", ",
    ) {
        printList(elements, separator) { el -> print(el) }
    }

    fun <T : NodeLike> printList(
        prefix: String,
        elements: List<T>,
        postfix: String,
        printEvenIfEmpty: Boolean = false,
        separator: String = ", ",
        elementPrinter: (T) -> Unit,
    ) {
        if (elements.isNotEmpty() || printEvenIfEmpty) {
            print(prefix)
            printList(elements, separator, elementPrinter)
            print(postfix)
        }
    }

    fun <T : NodeLike> printList(
        prefix: String,
        elements: List<T>,
        postfix: String,
        printEvenIfEmpty: Boolean = false,
        separator: String = ", ",
    ) {
        printList(prefix, elements, postfix, printEvenIfEmpty, separator) { el -> print(el) }
    }

    fun printOneOf(vararg alternatives: NodeLike?) {
        val notNull = alternatives.filterNotNull()
        if (notNull.size != 1) {
            throw IllegalStateException(
                "Expected exactly one alternative to be not null. " +
                    "Not null alternatives: $notNull",
            )
        }
        print(notNull.first())
    }
}
