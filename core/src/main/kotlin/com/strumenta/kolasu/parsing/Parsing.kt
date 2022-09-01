package com.strumenta.kolasu.parsing

import com.strumenta.kolasu.model.*
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import com.strumenta.kolasu.validation.Result
import java.io.*
import java.nio.charset.Charset
import java.util.*

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

class ParsingResult<RootNode : Node>(
    issues: List<Issue>,
    val root: RootNode?,
    code: String? = null,
    val incompleteNode: Node? = null,
    /**
     * It can contains intermediate results depending on how the parsing has been performed.
     * For example, a list of tokens or a parse tree.
     */
    val intermediateResults: Map<String, Any> = emptyMap(),
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

interface ASTParser<R : Node> {
    fun parse(
        inputStream: InputStream,
        charset: Charset = Charsets.UTF_8,
        considerPosition: Boolean = true,
        measureLexingTime: Boolean = false
    ):
        ParsingResult<R> = parse(inputStreamToString(inputStream, charset), considerPosition, measureLexingTime)
    fun parse(code: String, considerPosition: Boolean = true, measureLexingTime: Boolean = false): ParsingResult<R>

    fun parse(file: File, charset: Charset = Charsets.UTF_8, considerPosition: Boolean = true): ParsingResult<R>
}

private fun inputStreamToString(inputStream: InputStream, charset: Charset = Charsets.UTF_8): String =
    inputStream.bufferedReader(charset).use(BufferedReader::readText)
