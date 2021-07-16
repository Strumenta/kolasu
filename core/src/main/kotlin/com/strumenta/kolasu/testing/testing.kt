package com.strumenta.kolasu.testing

import com.strumenta.kolasu.parsing.toParseTree
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Vocabulary
import org.junit.jupiter.api.Assertions.assertEquals

fun assertParseTreeStr(expectedMultiLineStr: String, root: ParserRuleContext, vocabulary: Vocabulary,
                       printParseTree: Boolean = true) {
    val actualParseTree = toParseTree(root, vocabulary).multiLineString()
    if (printParseTree) {
        println("parse tree:\n\n${actualParseTree}\n")
    }
    assertEquals(expectedMultiLineStr, actualParseTree)
}
