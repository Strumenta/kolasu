package com.strumenta.kolasu.antlr.parsing

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Vocabulary
import kotlin.test.assertEquals

fun assertParseTreeStr(
    expectedMultiLineStr: String,
    root: ParserRuleContext,
    vocabulary: Vocabulary,
    printParseTree: Boolean = true
) {
    val actualParseTree = toParseTreeModel(root, vocabulary).multiLineString()
    if (printParseTree) {
        println("parse tree:\n\n${actualParseTree}\n")
    }
    assertEquals(expectedMultiLineStr.trim(), actualParseTree.trim())
}
