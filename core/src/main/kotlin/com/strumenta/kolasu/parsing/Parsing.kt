package com.strumenta.kolasu.parsing

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.endPoint
import com.strumenta.kolasu.model.startPoint
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueType
import org.antlr.v4.runtime.*
import java.io.*
import java.nio.charset.Charset
import java.util.*

open class CodeProcessingResult<D>(val issues: List<Issue>,
                                   val data: D?,
                                   val code: String? = null) {
    val correct: Boolean
        get() = issues.isEmpty()

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

abstract class KolasuParser<R: Node, P: Parser, C: ParserRuleContext> {

    protected abstract fun createANTLRLexer(inputStream: InputStream) : Lexer
    protected abstract fun createANTLRParser(tokenStream: TokenStream) : P
    protected abstract fun invokeRootRule(parser: P): C?
    protected abstract fun parseTreeToAst(parseTreeRoot: C, considerPosition: Boolean = true) : R?

    fun lex(inputStream: InputStream): LexingResult {
        val issues = LinkedList<Issue>()
        val lexer = createANTLRLexer(inputStream)
        lexer.removeErrorListeners()
        lexer.injectErrorCollectorInLexer(issues)
        val tokens = LinkedList<Token>()
        do {
            val t = lexer.nextToken()
            if (t == null) {
                break
            } else {
                tokens.add(t)
            }
        } while (t.type != Token.EOF)

        if (tokens.last.type != Token.EOF) {
            issues.add(Issue(IssueType.SYNTACTIC, "Not whole input consumed", tokens.last!!.endPoint.asPosition))
        }

        return LexingResult(issues, tokens)
    }


    fun createParser(inputStream: InputStream, issues: MutableList<Issue>): P {
        val lexer = createANTLRLexer(inputStream)
        lexer.removeErrorListeners()
        lexer.injectErrorCollectorInLexer(issues)
        val commonTokenStream = CommonTokenStream(lexer)
        val parser: P = createANTLRParser(commonTokenStream)
        parser.injectErrorCollectorInParser(issues)
        return parser
    }

    private fun verifyParseTree(parser: Parser, errors: MutableList<Issue>, root: ParserRuleContext) {
        val commonTokenStream = parser.tokenStream as CommonTokenStream
        val lastToken = commonTokenStream.get(commonTokenStream.index())
        if (lastToken.type != Token.EOF) {
            errors.add(Issue(IssueType.SYNTACTIC, "Not whole input consumed", lastToken!!.endPoint.asPosition))
        }

        root.processDescendantsAndErrors(
            {
                if (it.exception != null) {
                    errors.add(Issue(IssueType.SYNTACTIC, "Recognition exception: ${it.exception.message}", it.start.startPoint.asPosition))
                }
            },
            {
                errors.add(Issue(IssueType.SYNTACTIC, "Error node found", it.toPosition(true)))
            }
        )
    }

    fun parseFirstStage(code: String): FirstStageParsingResult<C> {
        return parseFirstStage(code.byteInputStream())
    }

    fun parseFirstStage(inputStream: InputStream): FirstStageParsingResult<C> {
        val issues = LinkedList<Issue>()
        val parser = createParser(inputStream, issues)
        val root: C? = invokeRootRule(parser)
        if (root != null) {
            verifyParseTree(parser, issues, root)
        }
        return FirstStageParsingResult(issues, root)
    }

    fun parseFirstStage(file: File): FirstStageParsingResult<C> = parseFirstStage(FileInputStream(file))

    private fun getAst(inputStream: InputStream, considerPosition: Boolean = true): R? {
        val result = parseFirstStage(inputStream)
        return parseTreeToAst(result.root!!, considerPosition)
    }

    fun parse(inputStream: InputStream, considerPosition: Boolean = true): ParsingResult<R> {
        val code = inputStreamToString(inputStream)
        val result = parseFirstStage(code)
        val ast = parseTreeToAst(result.root!!, considerPosition)
        return ParsingResult(result.issues, ast, code, null)
    }

    fun parse(code: String, considerPosition: Boolean = true): ParsingResult<R> {
        val result = parseFirstStage(code)
        val ast = parseTreeToAst(result.root!!, considerPosition)
        return ParsingResult(result.issues, ast, code, null)
    }

    fun parse(file: File, considerPosition: Boolean = true): ParsingResult<R> = parse(FileInputStream(file), considerPosition)
}

private fun inputStreamToString(inputStream: InputStream): String =
    inputStream.bufferedReader().use(BufferedReader::readText)