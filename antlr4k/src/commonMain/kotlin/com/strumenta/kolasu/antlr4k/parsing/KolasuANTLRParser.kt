package com.strumenta.kolasu.antlr4k.parsing

import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Range
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueType
import org.antlr.v4.kotlinruntime.BaseErrorListener
import org.antlr.v4.kotlinruntime.CommonToken
import org.antlr.v4.kotlinruntime.Parser
import org.antlr.v4.kotlinruntime.ParserRuleContext
import org.antlr.v4.kotlinruntime.RecognitionException
import org.antlr.v4.kotlinruntime.Recognizer

fun Parser.injectErrorCollectorInParser(issues: MutableList<Issue>) {
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
