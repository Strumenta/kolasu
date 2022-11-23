package com.strumenta.kolasu.transformation

import com.strumenta.kolasu.mapping.ParseTreeToASTTransformer
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.testing.assertASTsAreEqual
import com.strumenta.simplelang.AntlrEntityLexer
import com.strumenta.simplelang.AntlrEntityParser
import com.strumenta.simplelang.AntlrEntityParser.Boolean_typeContext
import com.strumenta.simplelang.AntlrEntityParser.EntityContext
import com.strumenta.simplelang.AntlrEntityParser.Entity_typeContext
import com.strumenta.simplelang.AntlrEntityParser.FeatureContext
import com.strumenta.simplelang.AntlrEntityParser.ModuleContext
import com.strumenta.simplelang.AntlrEntityParser.String_typeContext
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import org.junit.Test
import java.util.*

data class EModule(override val name: String, val entities: MutableList<EEntity>) : Node(), Named
data class EEntity(override val name: String, val features: MutableList<EFeature>) : Node(), Named

data class EFeature(override val name: String, val type: EType) : Node(), Named

sealed class EType: Node()
class EStringType : EType()
class EBooleanType : EType()
data class EEntityRefType(val entity: ReferenceByName<EEntity>) : EType()

class ParseTreeToASTTransformerTest {

    private fun parseEntities(code: String): AntlrEntityParser.ModuleContext {
        val errorListener = object : ANTLRErrorListener {
            override fun syntaxError(
                p0: Recognizer<*, *>?,
                p1: Any?,
                line: Int,
                column: Int,
                message: String?,
                p5: RecognitionException?
            ) {
                throw RuntimeException("L${line}:${column}: ${message}")
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
    fun testSimpleEntitiesTransformer() {
        val transformer = ParseTreeToASTTransformer(allowGenericNode = false)
        transformer.registerNodeFactory(
            ModuleContext::class,
            TrivialFactoryOfParseTreeToASTNodeFactory.trivialFactory<ModuleContext, EModule>()
        )
        transformer.registerNodeFactory(
            EntityContext::class,
            TrivialFactoryOfParseTreeToASTNodeFactory.trivialFactory<EntityContext, EEntity>()
        )
        val expectedAST = EModule(
            "M",
            mutableListOf(
                EEntity("FOO", mutableListOf()),
                EEntity("BAR", mutableListOf())
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

    @Test
    fun testEntitiesWithFeaturesTransformer() {
        val transformer = ParseTreeToASTTransformer(allowGenericNode = false)
        transformer.registerNodeFactory(
            ModuleContext::class,
            TrivialFactoryOfParseTreeToASTNodeFactory.trivialFactory<ModuleContext, EModule>()
        )
        transformer.registerNodeFactory(
            EntityContext::class,
            TrivialFactoryOfParseTreeToASTNodeFactory.trivialFactory<EntityContext, EEntity>()
        )
        transformer.registerNodeFactory(
            FeatureContext::class,
            TrivialFactoryOfParseTreeToASTNodeFactory.trivialFactory<FeatureContext, EFeature>()
        )
        transformer.registerNodeFactory(
            String_typeContext::class,
            TrivialFactoryOfParseTreeToASTNodeFactory.trivialFactory<String_typeContext, EStringType>()
        )
        transformer.registerNodeFactory(
            Boolean_typeContext::class,
            TrivialFactoryOfParseTreeToASTNodeFactory.trivialFactory<Boolean_typeContext, EBooleanType>()
        )
        transformer.registerNodeFactory(
            Entity_typeContext::class,
            TrivialFactoryOfParseTreeToASTNodeFactory.trivialFactory<Entity_typeContext, EEntityRefType>("target" to "entity")
        )
        val expectedAST = EModule(
            "M",
            mutableListOf(
                EEntity("FOO", mutableListOf(
                    EFeature("A", EStringType()),
                    EFeature("B", EBooleanType()),
                )),
                EEntity("BAR", mutableListOf(
                    EFeature("C", EEntityRefType(ReferenceByName("FOO")))
                )),
            )
        )
        val actualAST = transformer.transform(
            parseEntities(
                """
            module M {
               entity FOO {
                A: string;
                B: boolean;
                 
               }
               entity BAR {
                 C: FOO;
               }
            }
                """.trimIndent()
            )
        )!!
        assertASTsAreEqual(expectedAST, actualAST)
    }
}
