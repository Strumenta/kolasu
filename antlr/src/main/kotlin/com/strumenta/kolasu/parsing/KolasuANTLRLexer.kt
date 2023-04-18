package com.strumenta.kolasu.parsing

import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueType
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.Token
import java.io.InputStream
import java.nio.charset.Charset
import java.util.LinkedList
import kotlin.system.measureTimeMillis

abstract class KolasuANTLRLexer : KolasuLexer<KolasuANTLRToken> {
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
    protected abstract fun createANTLRLexer(charStream: CharStream): Lexer

    override fun lex(
        inputStream: InputStream,
        charset: Charset,
        onlyFromDefaultChannel: Boolean
    ): LexingResult<KolasuANTLRToken> {
        val issues = LinkedList<Issue>()
        val tokens = LinkedList<KolasuANTLRToken>()
        var last: Token? = null
        val time = measureTimeMillis {
            val lexer = createANTLRLexer(inputStream, charset)
            attachListeners(lexer, issues)
            do {
                val t = lexer.nextToken()
                if (t == null) {
                    break
                } else {
                    if (!onlyFromDefaultChannel || t.channel == Token.DEFAULT_CHANNEL) {
                        tokens.add(KolasuANTLRToken(categoryOf(t), t))
                        last = t
                    }
                }
            } while (t.type != Token.EOF)

            if (last != null && last!!.type != Token.EOF) {
                val message = "The parser didn't consume the entire input"
                issues.add(Issue(IssueType.SYNTACTIC, message, position = last!!.endPoint.asPosition))
            }
        }

        return LexingResult(issues, tokens, null, time)
    }

    protected open fun categoryOf(t: Token): TokenCategory = TokenCategory.PLAIN_TEXT

    protected open fun attachListeners(lexer: Lexer, issues: MutableList<Issue>) {
        lexer.injectErrorCollectorInLexer(issues)
    }
}

/**
 * A [KolasuToken] generated from a [Token]. The [token] contains additional information that is specific to ANTLR,
 * such as type and channel.
 */
data class KolasuANTLRToken(override val category: TokenCategory, val token: Token) :
    KolasuToken(category, token.position, token.text)

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
