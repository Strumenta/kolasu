package com.strumenta.kolasu.antlr4j.parsing

import com.strumenta.kolasu.ast.NodeLike
import com.strumenta.kolasu.parsing.CodeProcessingResult
import com.strumenta.kolasu.validation.Issue
import org.antlr.v4.runtime.ParserRuleContext

class FirstStageParsingResult<C : ParserRuleContext>(
    issues: List<Issue>,
    val root: C?,
    code: String? = null,
    val incompleteNode: NodeLike? = null,
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
