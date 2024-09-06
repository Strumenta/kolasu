package com.strumenta.kolasu.parsing

import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.Source
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import com.strumenta.kolasu.validation.Result

/**
 * The result of processing a piece of source code.
 * @param D the type of the transformed data.
 * @param issues a list of issues encountered while processing the code.
 * @param data the result of the process.
 * @param code the processed source code.
 * @param source where the source code comes from.
 */
open class CodeProcessingResult<D>(
    val issues: List<Issue>,
    val data: D?,
    val code: String? = null,
    val source: Source? = null,
) {
    val isCorrect: Boolean
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

/**
 * The result of lexing (tokenizing) a stream.
 */
class LexingResult<T : KolasuToken>(
    issues: List<Issue>,
    val tokens: List<T>,
    code: String? = null,
    val time: Long? = null,
    source: Source? = null,
) : CodeProcessingResult<List<T>>(issues, tokens, code, source) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LexingResult<*>) return false
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

open class ParsingResult<RootNode : NodeLike>(
    issues: List<Issue>,
    val root: RootNode?,
    code: String? = null,
    val incompleteNode: NodeLike? = null,
    val time: Long? = null,
    source: Source? = null,
) : CodeProcessingResult<RootNode>(issues, root, code, source) {
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
