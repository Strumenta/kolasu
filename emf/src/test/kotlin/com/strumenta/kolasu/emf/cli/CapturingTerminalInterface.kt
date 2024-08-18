package com.strumenta.kolasu.emf.cli

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.PrintRequest
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalInfo
import com.github.ajalt.mordant.terminal.TerminalInterface

fun capturingCliktConsole(): Pair<Terminal, CapturingTerminalInterface> {
    val cti = CapturingTerminalInterface()
    val terminal = Terminal(terminalInterface = cti)
    return terminal to cti
}

class CapturingTerminalInterface private constructor(
    override val info: TerminalInfo,
) : TerminalInterface {
    constructor(
        ansiLevel: AnsiLevel = AnsiLevel.TRUECOLOR,
        width: Int = 79,
        height: Int = 24,
        hyperlinks: Boolean = ansiLevel != AnsiLevel.NONE,
        outputInteractive: Boolean = ansiLevel != AnsiLevel.NONE,
        inputInteractive: Boolean = ansiLevel != AnsiLevel.NONE,
        crClearsLine: Boolean = false,
    ) : this(
        TerminalInfo(
            width = width,
            height = height,
            ansiLevel = ansiLevel,
            ansiHyperLinks = hyperlinks,
            outputInteractive = outputInteractive,
            inputInteractive = inputInteractive,
            crClearsLine = crClearsLine,
        ),
    )

    /**
     * Lines of input to return from [readLineOrNull].
     */
    var inputLines: MutableList<String> = mutableListOf()
    private val stdout: StringBuilder = StringBuilder()
    private val stderr: StringBuilder = StringBuilder()
    private val output: StringBuilder = StringBuilder()

    fun clearOutput() {
        stdout.clear()
        stderr.clear()
        output.clear()
    }

    /** The content written to stdout */
    fun stdout(): String = stdout.toString()

    /** The content written to stderr */
    fun stderr(): String = stderr.toString()

    /** The combined content of [stdout] and [stderr] */
    fun output(): String = output.toString()

    override fun completePrintRequest(request: PrintRequest) {
        val sb = if (request.stderr) stderr else stdout
        sb.append(request.text)
        output.append(request.text)
        if (request.trailingLinebreak) {
            sb.append("\n")
            output.append("\n")
        }
    }

    override fun readLineOrNull(hideInput: Boolean): String? = inputLines.removeFirstOrNull()
}
