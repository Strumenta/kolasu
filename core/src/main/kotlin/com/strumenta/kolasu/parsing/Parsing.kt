package com.strumenta.kolasu.parsing

import com.strumenta.kolasu.model.*
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueType
import org.antlr.v4.runtime.*
import java.io.*
import java.nio.charset.Charset
import java.util.*
import kotlin.reflect.full.memberFunctions
import kotlin.system.measureTimeMillis

open class CodeProcessingResult<D>(
    val issues: List<Issue>,
    val data: D?,
    val code: String? = null
) {
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
    val time: Long? = null
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

/**
 * A complete description of a multi-stage ANTLR-based parser, from source code to AST.
 *
 * You should extend this class to implement the parts that are specific to your language.
 */
abstract class KolasuParser<R : Node, P : Parser, C : ParserRuleContext> {

    protected abstract fun createANTLRLexer(inputStream: InputStream): Lexer
    protected abstract fun createANTLRParser(tokenStream: TokenStream): P

    /**
     * Invokes the parser's root rule, i.e., the method which is responsible of parsing the entire input.
     * Usually this is the topmost rule, the one with index 0 (as also assumed by other libraries such as antlr4-c3),
     * so this method invokes that rule. If your grammar/parser is structured differently, or if you're using this to
     * parse only a portion of the input or a subset of the language, you have to override this method to invoke the
     * correct entry point.
     */
    protected open fun invokeRootRule(parser: P): C? {
        val entryPoint = parser::class.memberFunctions.find { it.name == parser.ruleNames[0] }
        return entryPoint!!.call(parser) as C
    }

    protected abstract fun parseTreeToAst(parseTreeRoot: C, considerPosition: Boolean = true): R?

    @JvmOverloads
    fun lex(code: String, onlyFromDefaultChannel: Boolean = true): LexingResult {
        return lex(code.byteInputStream(Charsets.UTF_8), onlyFromDefaultChannel)
    }

    @JvmOverloads
    fun lex(inputStream: InputStream, onlyFromDefaultChannel: Boolean = true): LexingResult {
        val issues = LinkedList<Issue>()
        val tokens = LinkedList<Token>()
        val time = measureTimeMillis {
            val lexer = createANTLRLexer(inputStream)
            attachListeners(lexer, issues)
            do {
                val t = lexer.nextToken()
                if (t == null) {
                    break
                } else {
                    if (!onlyFromDefaultChannel || t.channel == Token.DEFAULT_CHANNEL) {
                        tokens.add(t)
                    }
                }
            } while (t.type != Token.EOF)

            if (tokens.last.type != Token.EOF) {
                val message = "The parser didn't consume the entire input"
                issues.add(Issue(IssueType.SYNTACTIC, message, tokens.last!!.endPoint.asPosition))
            }
        }

        return LexingResult(issues, tokens, null, time)
    }

    protected open fun attachListeners(lexer: Lexer, issues: MutableList<Issue>) {
        lexer.injectErrorCollectorInLexer(issues)
    }

    protected open fun attachListeners(parser: P, issues: MutableList<Issue>) {
        parser.injectErrorCollectorInParser(issues)
    }

    fun createParser(inputStream: InputStream, issues: MutableList<Issue>): P {
        val lexer = createANTLRLexer(inputStream)
        attachListeners(lexer, issues)
        val tokenStream = createTokenStream(lexer)
        val parser: P = createANTLRParser(tokenStream)
        attachListeners(parser, issues)
        return parser
    }

    protected open fun createTokenStream(lexer: Lexer) = CommonTokenStream(lexer)

    private fun verifyParseTree(parser: Parser, errors: MutableList<Issue>, root: ParserRuleContext) {
        val lastToken = parser.tokenStream.get(parser.tokenStream.index())
        if (lastToken.type != Token.EOF) {
            errors.add(Issue(IssueType.SYNTACTIC, "Not whole input consumed", lastToken!!.endPoint.asPosition))
        }

        root.processDescendantsAndErrors(
            {
                if (it.exception != null) {
                    errors.add(
                        Issue(
                            IssueType.SYNTACTIC, "Recognition exception: ${it.exception.message}",
                            it.start.startPoint.asPosition
                        )
                    )
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
        var root: C?
        val time = measureTimeMillis {
            val parser = createParser(inputStream, issues)
            root = invokeRootRule(parser)
            if (root != null) {
                verifyParseTree(parser, issues, root!!)
            }
        }
        return FirstStageParsingResult(issues, root, null, null, time)
    }

    fun parseFirstStage(file: File): FirstStageParsingResult<C> = parseFirstStage(FileInputStream(file))

    protected open fun postProcessAst(ast: R, issues: MutableList<Issue>): R {
        return ast
    }

    @JvmOverloads
    fun parse(inputStream: InputStream, considerPosition: Boolean = true): ParsingResult<R> {
        val time = System.currentTimeMillis()
        val code = inputStreamToString(inputStream)
        val result = parseFirstStage(code)
        var ast = parseTreeToAst(result.root!!, considerPosition)
        assignParents(ast)
        val myIssues = result.issues.toMutableList()
        ast = if (ast == null) null else postProcessAst(ast, myIssues)
        return ParsingResult(myIssues, ast, code, null, result, System.currentTimeMillis() - time)
    }

    @JvmOverloads
    fun parse(code: String, considerPosition: Boolean = true): ParsingResult<R> {
        val time = System.currentTimeMillis()
        val result = parseFirstStage(code)
        val ast = parseTreeToAst(result.root!!, considerPosition)
        assignParents(ast)
        return ParsingResult(result.issues, ast, code, null, result, System.currentTimeMillis() - time)
    }

    /**
     * Traverses the AST to ensure that parent nodes are correctly assigned.
     *
     * If you assign the parents correctly when you build the AST, or you're not interested in tracking child-parent
     * relationships, you can override this method to do nothing to improve performance.
     */
    protected open fun assignParents(ast: R?) {
        ast?.assignParents()
    }

    @JvmOverloads
    fun parse(file: File, considerPosition: Boolean = true): ParsingResult<R> = parse(
        FileInputStream(file),
        considerPosition
    )

    // For convenient use from Java
    fun walk(node: Node) = node.walk()

    @JvmOverloads
    fun processProperties(
        node: Node,
        propertyOperation: (PropertyDescription) -> Unit,
        propertiesToIgnore: Set<String> = DEFAULT_IGNORED_PROPERTIES
    ) = node.processProperties(propertiesToIgnore, propertyOperation)
}

private fun inputStreamToString(inputStream: InputStream): String =
    inputStream.bufferedReader().use(BufferedReader::readText)
