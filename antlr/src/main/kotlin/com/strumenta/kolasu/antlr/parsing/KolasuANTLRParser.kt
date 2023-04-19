package com.strumenta.kolasu.antlr.parsing

import com.strumenta.kolasu.model.*
import com.strumenta.kolasu.parsing.ASTParser
import com.strumenta.kolasu.parsing.KolasuToken
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.kolasu.traversing.walk
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueType
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.TokenStream
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
 */
abstract class KolasuANTLRParser<R : ASTNode, P : Parser, C : ParserRuleContext, T : KolasuToken>(
    tokenFactory: TokenFactory<T>
) : KolasuANTLRLexer<T>(tokenFactory), ASTParser<R> {

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
        considerPosition: Boolean = true,
        issues: MutableList<Issue>
    ): R?

    protected open fun attachListeners(parser: P, issues: MutableList<Issue>) {
        parser.injectErrorCollectorInParser(issues)
    }

    /**
     * Creates the first-stage parser.
     */
    protected open fun createParser(inputStream: CharStream, issues: MutableList<Issue>): P {
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
    protected open fun verifyParseTree(parser: Parser, issues: MutableList<Issue>, root: ParserRuleContext) {
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

    /**
     * Executes only the first stage of the parser, i.e., the production of a parse tree. Usually, you'll want to use
     * the [parse] method, that returns an AST which is simpler to use and query.
     */
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

    override fun parse(
        code: String,
        considerPosition: Boolean,
        measureLexingTime: Boolean
    ): ParsingResultWithFirstStage<R, C> {
        val inputStream = CharStreams.fromString(code)
        return parse(inputStream, considerPosition, measureLexingTime)
    }

    @JvmOverloads
    fun parse(
        inputStream: CharStream,
        considerPosition: Boolean = true,
        measureLexingTime: Boolean = false
    ): ParsingResultWithFirstStage<R, C> {
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
        return ParsingResultWithFirstStage(
            myIssues, ast, inputStream.getText(Interval(0, inputStream.index() + 1)),
            null, now - start, firstStage
        )
    }

    override fun parse(file: File, charset: Charset, considerPosition: Boolean): ParsingResult<R> =
        parse(FileInputStream(file), charset, considerPosition)

    // For convenient use from Java
    fun walk(node: ASTNode) = node.walk()

    @JvmOverloads
    fun processProperties(
        node: ASTNode,
        propertyOperation: (PropertyDescription) -> Unit,
        propertiesToIgnore: Set<String> = emptySet()
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

class ParsingResultWithFirstStage<RootNode : ASTNode, P : ParserRuleContext>(
    issues: List<Issue>,
    root: RootNode?,
    code: String? = null,
    incompleteNode: ASTNode? = null,
    time: Long? = null,
    val firstStage: FirstStageParsingResult<P>
) : ParsingResult<RootNode>(issues, root, code, incompleteNode, time)
