package com.strumenta.kolasu.antlr4k.testing

import com.strumenta.kolasu.antlr4k.parsing.toParseTreeModel
import org.antlr.v4.kotlinruntime.ParserRuleContext
import org.antlr.v4.kotlinruntime.Vocabulary
import kotlin.test.assertEquals

fun assertParseTreeStr(
    expectedMultiLineStr: String,
    root: ParserRuleContext,
    vocabulary: Vocabulary,
    printParseTree: Boolean = true,
) {
    val actualParseTree = toParseTreeModel(root, vocabulary).multiLineString()
    if (printParseTree) {
        println("parse tree:\n\n${actualParseTree}\n")
    }
    assertEquals(expectedMultiLineStr.trim(), actualParseTree.trim())
}
