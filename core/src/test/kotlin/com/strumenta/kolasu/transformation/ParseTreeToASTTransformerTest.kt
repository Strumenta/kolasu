package com.strumenta.kolasu.transformation

import com.strumenta.kolasu.mapping.ParseTreeToASTTransformer
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.testing.assertASTsAreEqual
import com.strumenta.simplelang.AntlrEntityLexer
import com.strumenta.simplelang.AntlrEntityParser
import com.strumenta.simplelang.AntlrEntityParser.ModuleContext
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import org.junit.Test
import java.util.*

data class EModule(override val name: String, val entities: MutableList<EEntity>) : Node(), Named
data class EEntity(override val name: String) : Node(), Named

class ParseTreeToASTTransformerTest {

    private fun parseEntities(code: String): AntlrEntityParser.ModuleContext {
        val errorListener = object : ANTLRErrorListener {
            override fun syntaxError(
                p0: Recognizer<*, *>?,
                p1: Any?,
                p2: Int,
                p3: Int,
                p4: String?,
                p5: RecognitionException?
            ) {
                throw RuntimeException()
            }

            override fun reportAmbiguity(
                p0: Parser?,
                p1: DFA?,
                p2: Int,
                p3: Int,
                p4: Boolean,
                p5: BitSet?,
                p6: ATNConfigSet?
            ) {
                // nothing to do
            }

            override fun reportAttemptingFullContext(
                p0: Parser?,
                p1: DFA?,
                p2: Int,
                p3: Int,
                p4: BitSet?,
                p5: ATNConfigSet?
            ) {
                // nothing to do
            }

            override fun reportContextSensitivity(p0: Parser?, p1: DFA?, p2: Int, p3: Int, p4: Int, p5: ATNConfigSet?) {
                // nothing to do
            }
        }

        val lexer = AntlrEntityLexer(CharStreams.fromString(code))
        lexer.removeErrorListeners()
        lexer.addErrorListener(errorListener)
        val parser = AntlrEntityParser(CommonTokenStream(lexer))
        parser.removeErrorListeners()
        parser.addErrorListener(errorListener)
        return parser.module()
    }

    @Test
    fun testEntitiesTransformer() {
        val transformer = ParseTreeToASTTransformer(allowGenericNode = false)
        transformer.registerNodeFactory(
            ModuleContext::class,
            TrivialFactoryOfParseTreeToASTNodeFactory.trivialFactory<ModuleContext, EModule>()
        )
//        transformer.registerNodeFactory(ModuleContext::class) { parseTree, ASTTransformer ->
//            EModule("Foo", mutableListOf())
//        }

        val expectedAST = EModule(
            "M",
            mutableListOf(
                EEntity("FOO"),
                EEntity("BAR"),
            )
        )
        val actualAST = transformer.transform(
            parseEntities(
                """
            module M {
               entity FOO { }
               entity BAR { }
            }
                """.trimIndent()
            )
        )!!
        assertASTsAreEqual(expectedAST, actualAST)
    }
}
