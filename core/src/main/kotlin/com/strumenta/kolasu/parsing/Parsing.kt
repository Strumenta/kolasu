package com.strumenta.kolasu.parsing

import com.strumenta.kolasu.model.*
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import com.strumenta.kolasu.validation.IssueType
import com.strumenta.kolasu.validation.Result
import org.antlr.v4.runtime.*
import java.io.*
import java.nio.charset.Charset

open class CodeProcessingResult<D>(
    val issues: List<Issue>,
    val data: D?,
    val code: String? = null
) {
    val correct: Boolean
        get() = issues.none { it.severity != IssueSeverity.INFO }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CodeProcessingResult<*>) return false

        if (issues != other.issues) return false
        if (data != other.data) return false
        if (code != other.code) return false

        return true
    }

    override fun hashCode(): Int {
        var result = issues.hashCode()
        result = 31 * result + (data?.hashCode() ?: 0)
        result = 31 * result + (code?.hashCode() ?: 0)
        return result
    }
}

class LexingResult(
    issues: List<Issue>,
    val tokens: List<Token>,
    code: String? = null,
    val time: Long? = null
) : CodeProcessingResult<List<Token>>(issues, tokens, code) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LexingResult) return false
        if (!super.equals(other)) return false

        if (tokens != other.tokens) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + tokens.hashCode()
        return result
    }
}

class FirstStageParsingResult<C : ParserRuleContext>(
    issues: List<Issue>,
    val root: C?,
    code: String? = null,
    val incompleteNode: Node? = null,
    val time: Long? = null,
    val lexingTime: Long? = null,
) : CodeProcessingResult<C>(issues, root, code) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FirstStageParsingResult<*>) return false
        if (!super.equals(other)) return false

        if (root != other.root) return false
        if (incompleteNode != other.incompleteNode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (root?.hashCode() ?: 0)
        result = 31 * result + (incompleteNode?.hashCode() ?: 0)
        return result
    }
}

class ParsingResult<RootNode : Node>(
    issues: List<Issue>,
    val root: RootNode?,
    code: String? = null,
    val incompleteNode: Node? = null,
    val firstStage: FirstStageParsingResult<*>? = null,
    val time: Long? = null
) : CodeProcessingResult<RootNode>(issues, root, code) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParsingResult<*>) return false
        if (!super.equals(other)) return false

        if (root != other.root) return false
        if (incompleteNode != other.incompleteNode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (root?.hashCode() ?: 0)
        result = 31 * result + (incompleteNode?.hashCode() ?: 0)
        return result
    }

    fun toResult(): Result<RootNode> = Result(issues, root)
}

fun String.toStream(charset: Charset = Charsets.UTF_8) = ByteArrayInputStream(toByteArray(charset))

interface KLexer {
    fun lex(code: String): LexingResult = lex(code.toStream())

    fun lex(file: File): LexingResult = lex(FileInputStream(file))

    fun lex(inputStream: InputStream): LexingResult
}

fun Lexer.injectErrorCollectorInLexer(issues: MutableList<Issue>) {
    this.removeErrorListeners()
    this.addErrorListener(object : BaseErrorListener() {
        override fun syntaxError(
            p0: Recognizer<*, *>?,
            p1: Any?,
            line: Int,
            charPositionInLine: Int,
            errorMessage: String?,
            p5: RecognitionException?
        ) {
            issues.add(
                Issue(
                    IssueType.LEXICAL,
                    errorMessage ?: "unspecified",
                    position = Point(line, charPositionInLine).asPosition
                )
            )
        }
    })
}

fun Parser.injectErrorCollectorInParser(issues: MutableList<Issue>) {
    this.removeErrorListeners()
    this.addErrorListener(object : BaseErrorListener() {
        override fun syntaxError(
            p0: Recognizer<*, *>?,
            p1: Any?,
            line: Int,
            charPositionInLine: Int,
            errorMessage: String?,
            p5: RecognitionException?
        ) {
            issues.add(
                Issue(
                    IssueType.SYNTACTIC,
                    errorMessage ?: "unspecified",
                    position = Point(line, charPositionInLine).asPosition
                )
            )
        }
    })
}
