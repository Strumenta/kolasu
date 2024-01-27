package com.strumenta.kolasu.antlr.parsing

import com.strumenta.kolasu.ast.Source
import com.strumenta.kolasu.model.FileSource
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.PropertyDescription
import com.strumenta.kolasu.model.Range
import com.strumenta.kolasu.model.assignParents
import com.strumenta.kolasu.model.processProperties
import com.strumenta.kolasu.parsing.ASTParser
import com.strumenta.kolasu.parsing.KolasuToken
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.kolasu.traversing.walk
import com.strumenta.kolasu.utils.capitalize
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueType
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonToken
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.TokenStream
import org.antlr.v4.runtime.atn.ParserATNSimulator
import org.antlr.v4.runtime.atn.PredictionContextCache
import org.antlr.v4.runtime.misc.Interval
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.util.LinkedList
import kotlin.reflect.full.memberFunctions
import kotlin.system.measureTimeMillis

/**
 * A complete description of a multi-stage ANTLR-based parser, from source code to AST.
 *
 * You should extend this class to implement the parts that are specific to your language.
 *
 * Note: instances of this class are thread-safe and they're meant to be reused. Do not create a new KolasuParser
 * instance every time you need to parse some source code, or performance may suffer.
 */
abstract class KolasuANTLRParser<R : NodeLike, P : Parser, C : ParserRuleContext, T : KolasuToken>(
    tokenFactory: TokenFactory<T>,
) : KolasuANTLRLexer<T>(tokenFactory),
    ASTParser<R> {
    protected var predictionContextCache = PredictionContextCache()

    /**
     * Creates the first-stage parser.
     */
    protected abstract fun createANTLRParser(tokenStream: TokenStream): P

    /**
     * Invokes the parser's root rule, i.e., the method which is responsible for parsing the entire input.
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
        considerRange: Boolean = true,
        issues: MutableList<Issue>,
        source: Source? = null,
    ): R?

    protected open fun attachListeners(
        parser: P,
        issues: MutableList<Issue>,
    ) {
        parser.injectErrorCollectorInParser(issues)
    }

    /**
     * Creates the first-stage parser.
     */
    protected open fun createParser(
        inputStream: CharStream,
        issues: MutableList<Issue>,
    ): P {
        val lexer = createANTLRLexer(inputStream)
        attachListeners(lexer, issues)
        val tokenStream = createTokenStream(lexer)
        val parser: P = createANTLRParser(tokenStream)
        // Assign interpreter to avoid caching DFA states indefinitely across executions
        parser.interpreter =
            ParserATNSimulator(parser, parser.atn, parser.interpreter.decisionToDFA, predictionContextCache)
        attachListeners(parser, issues)
        return parser
    }

    protected open fun createTokenStream(lexer: Lexer) = CommonTokenStream(lexer)

    /**
     * Checks the parse tree for correctness. If you're concerned about performance, you may want to override this to
     * do nothing.
     */
    protected open fun verifyParseTree(
        parser: Parser,
        issues: MutableList<Issue>,
        root: ParserRuleContext,
    ) {
        val lastToken = parser.tokenStream.get(parser.tokenStream.index())
        if (lastToken.type != Token.EOF) {
            issues.add(
                Issue(
                    IssueType.SYNTACTIC,
                    "The whole input was not consumed",
                    range = lastToken!!.endPoint.asRange,
                ),
            )
        }

        root.processDescendantsAndErrors(
            {
                if (it.exception != null) {
                    val message = "Recognition exception: ${it.exception.message}"
                    issues.add(Issue.syntactic(message, range = it.toRange()))
                }
            },
            {
                val message = "Error node found (token: ${it.symbol?.text})"
                issues.add(Issue.syntactic(message, range = it.toRange()))
            },
        )
    }

    @JvmOverloads
    fun parseFirstStage(
        code: String,
        measureLexingTime: Boolean = false,
    ): FirstStageParsingResult<C> {
        return parseFirstStage(CharStreams.fromString(code), measureLexingTime)
    }

    @JvmOverloads
    fun parseFirstStage(
        inputStream: InputStream,
        charset: Charset = Charsets.UTF_8,
        measureLexingTime: Boolean = false,
    ): FirstStageParsingResult<C> {
        return parseFirstStage(CharStreams.fromStream(inputStream, charset), measureLexingTime)
    }

    /**
     * Executes only the first stage of the parser, i.e., the production of a parse tree. Usually, you'll want to use
     * the [parse] method, that returns an AST which is simpler to use and query.
     */
    @JvmOverloads
    fun parseFirstStage(
        inputStream: CharStream,
        measureLexingTime: Boolean = false,
    ): FirstStageParsingResult<C> {
        val issues = LinkedList<Issue>()
        var root: C?
        var lexingTime: Long? = null
        val time =
            measureTimeMillis {
                val parser = createParser(inputStream, issues)
                countExecution(parser)
                if (measureLexingTime) {
                    val tokenStream = parser.inputStream
                    if (tokenStream is CommonTokenStream) {
                        lexingTime =
                            measureTimeMillis {
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
    fun parseFirstStage(
        file: File,
        charset: Charset = Charsets.UTF_8,
        measureLexingTime: Boolean = false,
    ): FirstStageParsingResult<C> = parseFirstStage(FileInputStream(file), charset, measureLexingTime)

    protected open fun postProcessAst(
        ast: R,
        issues: MutableList<Issue>,
    ): R {
        return ast
    }

    override fun parse(
        code: String,
        considerRange: Boolean,
        measureLexingTime: Boolean,
        source: Source?,
    ): ParsingResultWithFirstStage<R, C> {
        val inputStream = CharStreams.fromString(code)
        return parse(inputStream, considerRange, measureLexingTime, source)
    }

    @JvmOverloads
    fun parse(
        inputStream: CharStream,
        considerRange: Boolean = true,
        measureLexingTime: Boolean = false,
        source: Source? = null,
    ): ParsingResultWithFirstStage<R, C> {
        val start = System.currentTimeMillis()
        val firstStage = parseFirstStage(inputStream, measureLexingTime)
        val myIssues = firstStage.issues.toMutableList()
        var ast = parseTreeToAst(firstStage.root!!, considerRange, myIssues, source)

        assignParents(ast)
        ast = if (ast == null) null else postProcessAst(ast, myIssues)
        if (ast != null && !considerRange) {
            // Remove parseTreeNodes because they cause the range to be computed
            ast.walk().forEach { it.origin = null }
        }
        val now = System.currentTimeMillis()
        return ParsingResultWithFirstStage(
            myIssues,
            ast,
            inputStream.getText(Interval(0, inputStream.index() + 1)),
            null,
            now - start,
            firstStage,
        )
    }

    override fun parse(
        file: File,
        charset: Charset,
        considerRange: Boolean,
        measureLexingTime: Boolean,
    ): ParsingResult<R> =
        parse(
            FileInputStream(file),
            charset,
            considerRange = considerRange,
            measureLexingTime = measureLexingTime,
            FileSource(file),
        )

    // For convenient use from Java
    fun walk(node: NodeLike) = node.walk()

    @JvmOverloads
    fun processProperties(
        node: NodeLike,
        propertyOperation: (PropertyDescription) -> Unit,
        propertiesToIgnore: Set<String> = emptySet(),
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

    protected fun shouldWeClearCaches(): Boolean = executionsToNextCacheClean <= 0

    protected var executionCounter = 0
    var cacheCycleSize = 500
    var executionsToNextCacheClean = cacheCycleSize

    protected fun considerClearCaches() {
        if (shouldWeClearCaches()) {
            clearCaches()
        }
    }

    protected open fun countExecution(parser: Parser) {
        executionCounter++
        executionsToNextCacheClean--
        considerClearCaches()
    }

    open fun clearCaches() {
        executionsToNextCacheClean = cacheCycleSize
        val lexer = createANTLRLexer(CharStreams.fromString(""))
        lexer.interpreter.clearDFA()
        val parser = createANTLRParser(createTokenStream(lexer))
        parser.interpreter.clearDFA()
        predictionContextCache = PredictionContextCache()
    }
}

fun Parser.injectErrorCollectorInParser(issues: MutableList<Issue>) {
    this.removeErrorListeners()
    this.addErrorListener(
        object : BaseErrorListener() {
            override fun syntaxError(
                recognizer: Recognizer<*, *>?,
                offendingSymbol: Any?,
                line: Int,
                charPositionInLine: Int,
                errorMessage: String?,
                recognitionException: RecognitionException?,
            ) {
                val startPoint = Point(line, charPositionInLine)
                var endPoint = startPoint
                if (offendingSymbol is CommonToken) {
                    endPoint = offendingSymbol.endPoint
                }
                issues.add(
                    Issue(
                        IssueType.SYNTACTIC,
                        errorMessage?.capitalize() ?: "unspecified",
                        range = Range(startPoint, endPoint),
                    ),
                )
            }
        },
    )
}

class ParsingResultWithFirstStage<RootNode : NodeLike, P : ParserRuleContext>(
    issues: List<Issue>,
    root: RootNode?,
    code: String? = null,
    incompleteNode: NodeLike? = null,
    time: Long? = null,
    val firstStage: FirstStageParsingResult<P>,
) : ParsingResult<RootNode>(issues, root, code, incompleteNode, time)
