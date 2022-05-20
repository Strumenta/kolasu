package com.strumenta.kolasu.parsing

import com.strumenta.kolasu.model.*
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import com.strumenta.kolasu.validation.IssueType
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.misc.Interval
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

interface ASTParser<R : Node> {
    fun parse(
        inputStream: InputStream,
        charset: Charset = Charsets.UTF_8,
        considerPosition: Boolean = true,
        measureLexingTime: Boolean = false
    ):
        ParsingResult<R> = parse(inputStreamToString(inputStream, charset), considerPosition, measureLexingTime)
    fun parse(code: String, considerPosition: Boolean = true, measureLexingTime: Boolean = false): ParsingResult<R>
}

/**
 * A complete description of a multi-stage ANTLR-based parser, from source code to AST.
 *
 * You should extend this class to implement the parts that are specific to your language.
 */
abstract class KolasuParser<R : Node, P : Parser, C : ParserRuleContext> : ASTParser<R> {

    /**
     * Creates the lexer.
     */
    @JvmOverloads
    protected open fun createANTLRLexer(inputStream: InputStream, charset: Charset = Charsets.UTF_8): Lexer {
        return createANTLRLexer(CharStreams.fromStream(inputStream, charset))
    }

    /**
     * Creates the lexer.
     */
    protected abstract fun createANTLRLexer(inputStream: CharStream): Lexer

    /**
     * Creates the first-stage parser.
     */
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

    /**
     * Transforms a parse tree into an AST (second parsing stage).
     */
    protected abstract fun parseTreeToAst(
        parseTreeRoot: C,
        considerPosition: Boolean = true,
        issues: MutableList<Issue>
    ): R?

    /**
     * Performs "lexing" on the given code string, i.e., it breaks it into tokens.
     */
    @JvmOverloads
    fun lex(code: String, onlyFromDefaultChannel: Boolean = true): LexingResult {
        val charset = Charsets.UTF_8
        return lex(code.byteInputStream(charset), charset, onlyFromDefaultChannel)
    }

    /**
     * Performs "lexing" on the given code stream, i.e., it breaks it into tokens.
     */
    @JvmOverloads
    fun lex(
        inputStream: InputStream,
        charset: Charset = Charsets.UTF_8,
        onlyFromDefaultChannel: Boolean = true
    ): LexingResult {
        val issues = LinkedList<Issue>()
        val tokens = LinkedList<Token>()
        val time = measureTimeMillis {
            val lexer = createANTLRLexer(inputStream, charset)
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
                issues.add(Issue(IssueType.SYNTACTIC, message, position = tokens.last!!.endPoint.asPosition))
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

    /**
     * Creates the first-stage lexer and parser.
     */
    protected fun createParser(inputStream: CharStream, issues: MutableList<Issue>): P {
        val lexer = createANTLRLexer(inputStream)
        attachListeners(lexer, issues)
        val tokenStream = createTokenStream(lexer)
        val parser: P = createANTLRParser(tokenStream)
        attachListeners(parser, issues)
        return parser
    }

    protected open fun createTokenStream(lexer: Lexer) = CommonTokenStream(lexer)

    /**
     * Checks the parse tree for correctness. If you're concerned about performance, you may want to override this to
     * do nothing.
     */
    open fun verifyParseTree(parser: Parser, issues: MutableList<Issue>, root: ParserRuleContext) {
        val lastToken = parser.tokenStream.get(parser.tokenStream.index())
        if (lastToken.type != Token.EOF) {
            issues.add(
                Issue(
                    IssueType.SYNTACTIC, "The whole input was not consumed",
                    position = lastToken!!.endPoint.asPosition
                )
            )
        }

        root.processDescendantsAndErrors(
            {
                if (it.exception != null) {
                    val message = "Recognition exception: ${it.exception.message}"
                    issues.add(Issue.syntactic(message, position = it.toPosition()))
                }
            },
            {
                val message = "Error node found (token: ${it.symbol?.text})"
                issues.add(Issue.syntactic(message, position = it.toPosition()))
            }
        )
    }

    @JvmOverloads
    fun parseFirstStage(code: String, measureLexingTime: Boolean = false): FirstStageParsingResult<C> {
        return parseFirstStage(CharStreams.fromString(code), measureLexingTime)
    }

    @JvmOverloads
    fun parseFirstStage(
        inputStream: InputStream,
        charset: Charset = Charsets.UTF_8,
        measureLexingTime: Boolean = false
    ): FirstStageParsingResult<C> {
        return parseFirstStage(CharStreams.fromStream(inputStream, charset), measureLexingTime)
    }

    @JvmOverloads
    fun parseFirstStage(inputStream: CharStream, measureLexingTime: Boolean = false): FirstStageParsingResult<C> {
        val issues = LinkedList<Issue>()
        var root: C?
        var lexingTime: Long? = null
        val time = measureTimeMillis {
            val parser = createParser(inputStream, issues)
            if (measureLexingTime) {
                val tokenStream = parser.inputStream
                if (tokenStream is CommonTokenStream) {
                    lexingTime = measureTimeMillis {
                        tokenStream.fill()
                        tokenStream.seek(0)
                    }
                }
            }
            root = invokeRootRule(parser)
            if (root != null) {
                verifyParseTree(parser, issues, root!!)
            }
        }
        return FirstStageParsingResult(issues, root, null, null, time, lexingTime)
    }

    @JvmOverloads
    fun parseFirstStage(file: File, charset: Charset = Charsets.UTF_8, measureLexingTime: Boolean = false):
        FirstStageParsingResult<C> =
        parseFirstStage(FileInputStream(file), charset, measureLexingTime)

    protected open fun postProcessAst(ast: R, issues: MutableList<Issue>): R {
        return ast
    }

    @JvmOverloads
    override fun parse(code: String, considerPosition: Boolean, measureLexingTime: Boolean): ParsingResult<R> {
        val inputStream = CharStreams.fromString(code)
        return parse(inputStream, considerPosition, measureLexingTime)
    }

    @JvmOverloads
    fun parse(
        inputStream: CharStream,
        considerPosition: Boolean = true,
        measureLexingTime: Boolean = false
    ): ParsingResult<R> {
        val start = System.currentTimeMillis()
        val firstStage = parseFirstStage(inputStream, measureLexingTime)
        val myIssues = firstStage.issues.toMutableList()
        var ast = parseTreeToAst(firstStage.root!!, considerPosition, myIssues)
        assignParents(ast)
        ast = if (ast == null) null else postProcessAst(ast, myIssues)
        if (ast != null && !considerPosition) {
            // Remove parseTreeNodes because they cause the position to be computed
            ast.walk().forEach { it.origin = null }
        }
        val now = System.currentTimeMillis()
        return ParsingResult(
            myIssues, ast, inputStream.getText(Interval(0, inputStream.index() + 1)),
            null, firstStage, now - start
        )
    }

    @JvmOverloads
    fun parse(file: File, charset: Charset = Charsets.UTF_8, considerPosition: Boolean = true): ParsingResult<R> =
        parse(FileInputStream(file), charset, considerPosition)

    // For convenient use from Java
    fun walk(node: Node) = node.walk()

    @JvmOverloads
    fun processProperties(
        node: Node,
        propertyOperation: (PropertyDescription) -> Unit,
        propertiesToIgnore: Set<String> = DEFAULT_IGNORED_PROPERTIES
    ) = node.processProperties(propertiesToIgnore, propertyOperation)

    /**
     * Traverses the AST to ensure that parent nodes are correctly assigned.
     *
     * If you're already assigning the parents correctly when you build the AST, or you're not interested in tracking
     * child-parent relationships, you can override this method to do nothing to improve performance.
     */
    protected open fun assignParents(ast: R?) {
        ast?.assignParents()
    }
}

private fun inputStreamToString(inputStream: InputStream, charset: Charset = Charsets.UTF_8): String =
    inputStream.bufferedReader(charset).use(BufferedReader::readText)
