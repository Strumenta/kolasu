package com.strumenta.kolasu.antlr4k.parsing

import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.parsing.KolasuToken
import com.strumenta.kolasu.parsing.LexingResult
import com.strumenta.kolasu.parsing.TokenCategory
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueType
import org.antlr.v4.kotlinruntime.BaseErrorListener
import org.antlr.v4.kotlinruntime.Lexer
import org.antlr.v4.kotlinruntime.RecognitionException
import org.antlr.v4.kotlinruntime.Recognizer
import org.antlr.v4.kotlinruntime.Token
import org.antlr.v4.kotlinruntime.tree.ParseTree
import org.antlr.v4.kotlinruntime.tree.TerminalNode

interface TokenFactory<T : KolasuToken> {
    fun categoryOf(t: Token): TokenCategory = TokenCategory.PLAIN_TEXT

    fun convertToken(t: Token): T

    private fun convertToken(terminalNode: TerminalNode): T = convertToken(terminalNode.symbol)

    fun extractTokens(result: FirstStageParsingResult<*>): LexingResult<T>? {
        val antlrTerminals = mutableListOf<TerminalNode>()

        fun extractTokensFromParseTree(pt: ParseTree?) {
            if (pt is TerminalNode) {
                antlrTerminals.add(pt)
            } else if (pt != null) {
                for (i in 0..pt.childCount) {
                    extractTokensFromParseTree(pt.getChild(i))
                }
            }
        }

        val ptRoot = result.root
        return if (ptRoot != null) {
            extractTokensFromParseTree(ptRoot)
            antlrTerminals.sortBy { it.symbol.tokenIndex }
            val tokens = antlrTerminals.map { convertToken(it) }.toMutableList()
            LexingResult(result.issues, tokens, result.code, result.lexingTime)
        } else {
            null
        }
    }
}

class ANTLRTokenFactory : TokenFactory<KolasuANTLRToken> {
    override fun convertToken(t: Token): KolasuANTLRToken = KolasuANTLRToken(categoryOf(t), t)
}

// abstract class KolasuANTLRLexer<T : KolasuToken>(
//    val tokenFactory: TokenFactory<T>,
// ) : KolasuLexer<T> {
//    /**
//     * Creates the lexer.
//     */
//    @JvmOverloads
//    protected open fun createANTLRLexer(
//        inputStream: InputStream,
//        charset: Charset = Charsets.UTF_8,
//    ): Lexer {
//        return createANTLRLexer(CharStreams.fromStream(inputStream, charset))
//    }
//
//    /**
//     * Creates the lexer.
//     */
//    protected abstract fun createANTLRLexer(charStream: CharStream): Lexer
//
//    override fun lex(
//        inputStream: InputStream,
//        charset: Charset,
//        onlyFromDefaultChannel: Boolean,
//    ): LexingResult<T> {
//        val issues = mutableListOf<Issue>()
//        val tokens = mutableListOf<T>()
//        var last: Token? = null
//        val time =
//            measureTimeMillis {
//                val lexer = createANTLRLexer(inputStream, charset)
//                attachListeners(lexer, issues)
//                do {
//                    val t = lexer.nextToken()
//                    if (t == null) {
//                        break
//                    } else {
//                        if (!onlyFromDefaultChannel || t.channel == Token.DEFAULT_CHANNEL) {
//                            tokens.add(tokenFactory.convertToken(t))
//                            last = t
//                        }
//                    }
//                } while (t.type != Token.EOF)
//
//                if (last != null && last!!.type != Token.EOF) {
//                    val message = "The parser didn't consume the entire input"
//                    issues.add(Issue(IssueType.SYNTACTIC, message, range = last!!.endPoint.asRange))
//                }
//            }
//
//        return LexingResult(issues, tokens, null, time)
//    }
//
//    protected open fun categoryOf(t: Token): TokenCategory = TokenCategory.PLAIN_TEXT
//
//    protected open fun attachListeners(
//        lexer: Lexer,
//        issues: MutableList<Issue>,
//    ) {
//        lexer.injectErrorCollectorInLexer(issues)
//    }
// }

/**
 * A [KolasuToken] generated from a [Token]. The [token] contains additional information that is specific to ANTLR,
 * such as type and channel.
 */
data class KolasuANTLRToken(
    override val category: TokenCategory,
    val token: Token,
) : KolasuToken(category, token.range, token.text!!)

fun Lexer.injectErrorCollectorInLexer(issues: MutableList<Issue>) {
    this.removeErrorListeners()
    this.addErrorListener(
        object : BaseErrorListener() {
            override fun syntaxError(
                recognizer: Recognizer<*, *>,
                offendingSymbol: Any?,
                line: Int,
                charPositionInLine: Int,
                errorMessage: String,
                e: RecognitionException?,
            ) {
                issues.add(
                    Issue(
                        IssueType.LEXICAL,
                        errorMessage ?: "unspecified",
                        range = Point(line, charPositionInLine).asRange,
                    ),
                )
            }
        },
    )
}
