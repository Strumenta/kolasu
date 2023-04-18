package com.strumenta.kolasu.transformation

import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.children
import com.strumenta.kolasu.testing.assertASTsAreEqual
import com.strumenta.kolasu.traversing.walk
import com.strumenta.simplelang.AntlrEntityLexer
import com.strumenta.simplelang.AntlrEntityParser
import com.strumenta.simplelang.AntlrEntityParser.Boolean_typeContext
import com.strumenta.simplelang.AntlrEntityParser.EntityContext
import com.strumenta.simplelang.AntlrEntityParser.Entity_typeContext
import com.strumenta.simplelang.AntlrEntityParser.FeatureContext
import com.strumenta.simplelang.AntlrEntityParser.ModuleContext
import com.strumenta.simplelang.AntlrEntityParser.String_typeContext
import com.strumenta.simplelang.AntlrScriptLexer
import com.strumenta.simplelang.AntlrScriptParser
import com.strumenta.simplelang.AntlrScriptParser.Concat_expressionContext
import com.strumenta.simplelang.AntlrScriptParser.Create_statementContext
import com.strumenta.simplelang.AntlrScriptParser.Div_mult_expressionContext
import com.strumenta.simplelang.AntlrScriptParser.Entity_by_id_expressionContext
import com.strumenta.simplelang.AntlrScriptParser.Feature_access_expressionContext
import com.strumenta.simplelang.AntlrScriptParser.Int_literal_expressionContext
import com.strumenta.simplelang.AntlrScriptParser.Parens_expressionContext
import com.strumenta.simplelang.AntlrScriptParser.Print_statementContext
import com.strumenta.simplelang.AntlrScriptParser.ScriptContext
import com.strumenta.simplelang.AntlrScriptParser.Set_statementContext
import com.strumenta.simplelang.AntlrScriptParser.String_literal_expressionContext
import com.strumenta.simplelang.AntlrScriptParser.Sum_sub_expressionContext
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import org.junit.Test
import java.util.*
import kotlin.test.assertSame

data class EModule(override val name: String, val entities: MutableList<EEntity>) : Node(), Named
data class EEntity(override val name: String, val features: MutableList<EFeature>) : Node(), Named

data class EFeature(override val name: String, val type: EType) : Node(), Named

sealed class EType : Node()
class EStringType : EType()
class EBooleanType : EType()
data class EEntityRefType(val entity: ReferenceByName<EEntity>) : EType()

data class SScript(val statements: MutableList<SStatement>) : Node()
sealed class SStatement : Node()
data class SCreateStatement(val entity: ReferenceByName<EEntity>, val name: String? = null) : SStatement()
data class SSetStatement(val feature: ReferenceByName<EFeature>, val instance: SExpression, val value: SExpression) :
    SStatement()
data class SPrintStatement(val message: SExpression) : SStatement()

sealed class SExpression : Node()
data class SStringLiteral(val value: String) : SExpression()
data class SIntegerLiteral(val value: Int) : SExpression()
data class SDivision(val left: SExpression, val right: SExpression) : SExpression()
data class SSubtraction(val left: SExpression, val right: SExpression) : SExpression()
data class SMultiplication(val left: SExpression, val right: SExpression) : SExpression()
data class SSum(val left: SExpression, val right: SExpression) : SExpression()
data class SConcat(val left: SExpression, val right: SExpression) : SExpression()
data class SFeatureAccess(val feature: ReferenceByName<EFeature>, val container: SExpression) : SExpression()
data class SInstanceById(val entity: ReferenceByName<EEntity>, val index: SExpression) : SExpression()

class ParseTreeToASTTransformerTest {

    class MyErrorListener : ANTLRErrorListener {
        override fun syntaxError(
            p0: Recognizer<*, *>?,
            p1: Any?,
            line: Int,
            column: Int,
            message: String?,
            p5: RecognitionException?
        ) {
            throw RuntimeException("L$line:$column: $message")
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

    private fun parseEntities(code: String): ModuleContext {
        val errorListener = MyErrorListener()

        val lexer = AntlrEntityLexer(CharStreams.fromString(code))
        lexer.removeErrorListeners()
        lexer.addErrorListener(errorListener)
        val parser = AntlrEntityParser(CommonTokenStream(lexer))
        parser.removeErrorListeners()
        parser.addErrorListener(errorListener)
        return parser.module()
    }

    private fun parseScript(code: String): ScriptContext {
        val errorListener = MyErrorListener()

        val lexer = AntlrScriptLexer(CharStreams.fromString(code))
        lexer.removeErrorListeners()
        lexer.addErrorListener(errorListener)
        val parser = AntlrScriptParser(CommonTokenStream(lexer))
        parser.removeErrorListeners()
        parser.addErrorListener(errorListener)
        return parser.script()
    }

    @Test
    fun testSimpleEntitiesTransformer() {
        val transformer = ParseTreeToASTTransformer(allowGenericNode = false)
        transformer.registerTrivialPTtoASTConversion<ModuleContext, EModule>()
        transformer.registerTrivialPTtoASTConversion<EntityContext, EEntity>()
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
        transformer.registerTrivialPTtoASTConversion<ModuleContext, EModule>()
        transformer.registerTrivialPTtoASTConversion<EntityContext, EEntity>()
        transformer.registerTrivialPTtoASTConversion<FeatureContext, EFeature>()
        transformer.registerTrivialPTtoASTConversion<String_typeContext, EStringType>()
        transformer.registerTrivialPTtoASTConversion<Boolean_typeContext, EBooleanType>()
        transformer.registerTrivialPTtoASTConversion<Entity_typeContext, EEntityRefType>("target" to "entity")
        val expectedAST = EModule(
            "M",
            mutableListOf(
                EEntity(
                    "FOO",
                    mutableListOf(
                        EFeature("A", EStringType()),
                        EFeature("B", EBooleanType()),
                    )
                ),
                EEntity(
                    "BAR",
                    mutableListOf(
                        EFeature("C", EEntityRefType(ReferenceByName("FOO")))
                    )
                ),
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

    @Test
    fun testScriptTransformer() {
        val transformer = ParseTreeToASTTransformer(allowGenericNode = false)
        transformer.registerTrivialPTtoASTConversion<ScriptContext, SScript>()
        transformer.registerTrivialPTtoASTConversion<Create_statementContext, SCreateStatement>(
            Create_statementContext::var_name to SCreateStatement::name
        )
        transformer.registerTrivialPTtoASTConversion<Set_statementContext, SSetStatement>()
        transformer.registerTrivialPTtoASTConversion<Entity_by_id_expressionContext, SInstanceById>(
            Entity_by_id_expressionContext::id to SInstanceById::index
        )
        transformer.registerTrivialPTtoASTConversion<Int_literal_expressionContext, SIntegerLiteral>(
            Int_literal_expressionContext::INT_VALUE to SIntegerLiteral::value
        )
        transformer.registerNodeTransformer(String_literal_expressionContext::class) { pt, t ->
            SStringLiteral(pt.text.removePrefix("'").removeSuffix("'"))
        }
        transformer.registerNodeTransformer(Div_mult_expressionContext::class) { pt, t ->
            when (pt.op.text) {
                "/" -> {
                    TrivialFactoryOfParseTreeToASTNodeTransformer.trivialTransformer<
                        Div_mult_expressionContext, SDivision>()(pt, t)
                }
                "*" -> {
                    TrivialFactoryOfParseTreeToASTNodeTransformer.trivialTransformer<
                        Div_mult_expressionContext, SMultiplication>()(pt, t)
                }
                else -> TODO()
            }
        }
        transformer.registerNodeTransformer(Sum_sub_expressionContext::class) { pt, t ->
            when (pt.op.text) {
                "+" -> {
                    TrivialFactoryOfParseTreeToASTNodeTransformer.trivialTransformer<
                        Sum_sub_expressionContext, SSum>()(pt, t)
                }
                "-" -> {
                    TrivialFactoryOfParseTreeToASTNodeTransformer.trivialTransformer<
                        Sum_sub_expressionContext, SSubtraction>()(pt, t)
                }
                else -> TODO()
            }
        }
        transformer.unwrap<Parens_expressionContext, SExpression>(Parens_expressionContext::expression)
        transformer.registerTrivialPTtoASTConversion<Print_statementContext, SPrintStatement>()
        transformer.registerTrivialPTtoASTConversion<Concat_expressionContext, SConcat>()
        transformer.registerTrivialPTtoASTConversion<Feature_access_expressionContext, SFeatureAccess>(
            Feature_access_expressionContext::instance to SFeatureAccess::container
        )
        val expectedAST = SScript(
            mutableListOf(
                SCreateStatement(ReferenceByName("Client")),
                SSetStatement(
                    ReferenceByName("name"),
                    SInstanceById(
                        ReferenceByName("Client"),
                        SIntegerLiteral(1)
                    ),
                    SStringLiteral("ACME Inc.")
                ),
                SCreateStatement(ReferenceByName("Product")),
                SSetStatement(
                    ReferenceByName("value"),
                    SInstanceById(
                        ReferenceByName("Product"),
                        SIntegerLiteral(2)
                    ),
                    SDivision(SSum(SIntegerLiteral(1500), SIntegerLiteral(200)), SIntegerLiteral(2))
                ),
                SPrintStatement(
                    SConcat(
                        SStringLiteral("Value of Product #2 is: "),
                        SFeatureAccess(
                            ReferenceByName("value"),
                            SInstanceById(
                                ReferenceByName("Product"),
                                SIntegerLiteral(2)
                            )
                        )
                    )
                )
            )
        )
        val actualAST = transformer.transform(
            parseScript(
                """create Client
                set name of Client #1 to 'ACME Inc.'
                create Product
                set value of Product #2 to (1500 + 200) / 2
                print concat 'Value of Product #2 is: ' and value of Product #2
                """.trimIndent()
            )
        )!!
        actualAST.walk().forEach { parent ->
            parent.children.forEach { child ->
                assertSame(parent, child.parent)
            }
        }
        assertASTsAreEqual(expectedAST, actualAST)
    }
}
