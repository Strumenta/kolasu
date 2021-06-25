package com.strumenta.kolasu.parsing

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueType
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.Parser
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.Charset

data class ParsingResult<RootNode : Node>(
    val root: RootNode?,
    val issues: List<Issue>,
    val code: String,
    val incompleteNode: Node? = null
) {
    fun isCorrect() = issues.isEmpty() && root != null
}

fun String.toStream(charset: Charset = Charsets.UTF_8) = ByteArrayInputStream(toByteArray(charset))

interface Parser<RootNode : Node> {
    fun parse(code: String, withValidation: Boolean = true): ParsingResult<RootNode> = parse(
        code.toStream(),
        withValidation
    )

    fun parse(file: File, withValidation: Boolean = true): ParsingResult<RootNode> = parse(
        FileInputStream(file),
        withValidation
    )

    fun parse(inputStream: InputStream, withValidation: Boolean = true): ParsingResult<RootNode>
}

fun injectErrorCollectorInLexer(lexer: Lexer, errors: MutableList<Issue>) {
    lexer.removeErrorListeners()
    lexer.addErrorListener(object : BaseErrorListener() {
        override fun syntaxError(
            p0: Recognizer<*, *>?,
            p1: Any?,
            line: Int,
            charPositionInLine: Int,
            errorMessage: String?,
            p5: RecognitionException?
        ) {
            errors.add(
                Issue(
                    IssueType.LEXICAL,
                    errorMessage ?: "unspecified",
                    position = Point(line, charPositionInLine).asPosition
                )
            )
        }
    })
}

fun injectErrorCollectorInParser(parser: Parser, errors: MutableList<Issue>) {
    parser.removeErrorListeners()
    parser.addErrorListener(object : BaseErrorListener() {
        override fun syntaxError(
            p0: Recognizer<*, *>?,
            p1: Any?,
            line: Int,
            charPositionInLine: Int,
            errorMessage: String?,
            p5: RecognitionException?
        ) {
            errors.add(
                Issue(
                    IssueType.SYNTACTIC,
                    errorMessage ?: "unspecified",
                    position = Point(line, charPositionInLine).asPosition
                )
            )
        }
    })
}
