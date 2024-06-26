package com.strumenta.kolasu.antlr.mapping

import com.strumenta.kolasu.antlr4j.mapping.ParseTreeToASTTransformer
import com.strumenta.kolasu.antlr4j.mapping.TrivialFactoryOfParseTreeToASTNodeTransformer
import com.strumenta.kolasu.antlr4j.mapping.registerTrivialPTtoASTConversion
import com.strumenta.kolasu.antlr4j.mapping.unwrap
import com.strumenta.kolasu.antlr4j.parsing.withParseTreeNode
import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.language.explore
import com.strumenta.kolasu.model.GenericErrorNode
import com.strumenta.kolasu.model.Internal
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.Range
import com.strumenta.kolasu.model.ReferenceValue
import com.strumenta.kolasu.model.children
import com.strumenta.kolasu.model.hasValidParents
import com.strumenta.kolasu.model.invalidRanges
import com.strumenta.kolasu.testing.assertASTsAreEqual
import com.strumenta.kolasu.transformation.ASTTransformer
import com.strumenta.kolasu.transformation.GenericNode
import com.strumenta.kolasu.traversing.walk
import com.strumenta.kolasu.validation.Issue
import com.strumenta.simplelang.AntlrEntityLexer
import com.strumenta.simplelang.AntlrEntityParser
import com.strumenta.simplelang.AntlrScriptLexer
import com.strumenta.simplelang.AntlrScriptParser
import com.strumenta.simplelang.SimpleLangLexer
import com.strumenta.simplelang.SimpleLangParser
import org.antlr.v4.runtime.ANTLRErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import org.junit.Test
import java.util.BitSet
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

object StarLasuLanguageInstance : StarLasuLanguage("com.strumenta.kolasu.antlr.mapping") {
    init {
        explore(
            CU::class, SetStatement::class, DisplayIntStatement::class,
            SCreateStatement::class, SIntegerLiteral::class, SInstanceById::class,
            SSetStatement::class, SPrintStatement::class, SScript::class,
            EModule::class, Ent::class, EntStringType::class,
        )
    }
}

data class CU(
    @property:Internal
    val specifiedRange: Range? = null,
    var statements: List<NodeLike> = listOf(),
) : Node(specifiedRange)

data class DisplayIntStatement(
    @property:Internal
    val specifiedRange: Range? = null,
    val value: Int,
) : Node(specifiedRange)

data class SetStatement(
    @property:Internal
    val specifiedRange: Range? = null,
    var variable: String = "",
    val value: Int = 0,
) : Node(specifiedRange)

data class EModule(
    override val name: String,
    val entities: MutableList<EEntity>,
) : Node(),
    Named

data class EEntity(
    override val name: String,
    val features2: MutableList<EFeature>,
) : Node(),
    Named

data class EFeature(
    override val name: String,
    val type: EType,
) : Node(),
    Named

sealed class EType : Node()

class EStringType : EType()

class EBooleanType : EType()

data class EEntityRefType(
    val entity: ReferenceValue<EEntity>,
) : EType()

data class SScript(
    val statements: MutableList<SStatement>,
) : Node()

sealed class SStatement : Node()

data class SCreateStatement(
    val entity: ReferenceValue<EEntity>,
    val name: String? = null,
) : SStatement()

data class SSetStatement(
    val feature: ReferenceValue<EFeature>,
    val instance: SExpression,
    val value: SExpression,
) : SStatement()

data class SPrintStatement(
    val message: SExpression,
) : SStatement()

sealed class SExpression : Node()

data class SStringLiteral(
    val value: String,
) : SExpression()

data class SIntegerLiteral(
    val value: Int,
) : SExpression()

data class SDivision(
    val left: SExpression,
    val right: SExpression,
) : SExpression()

data class SSubtraction(
    val left: SExpression,
    val right: SExpression,
) : SExpression()

data class SMultiplication(
    val left: SExpression,
    val right: SExpression,
) : SExpression()

data class SSum(
    val left: SExpression,
    val right: SExpression,
) : SExpression()

data class SConcat(
    val left: SExpression,
    val right: SExpression,
) : SExpression()

data class SFeatureAccess(
    val feature: ReferenceValue<EFeature>,
    val container: SExpression,
) : SExpression()

data class SInstanceById(
    val entity: ReferenceValue<EEntity>,
    val index: SExpression,
) : SExpression()

class ParseTreeToASTTransformerTest {
    init {
        StarLasuLanguageInstance.ensureIsRegistered()
    }

    @Test
    fun testParseTreeTransformer() {
        val code = "set foo = 123\ndisplay 456"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val pt = parser.compilationUnit()

        val transformer = ParseTreeToASTTransformer()
        configure(transformer)

        val cu =
            CU(
                statements =
                    listOf(
                        SetStatement(variable = "foo", value = 123).withParseTreeNode(pt.statement(0)),
                        DisplayIntStatement(value = 456).withParseTreeNode(pt.statement(1)),
                    ),
            ).withParseTreeNode(pt)
        val transformedCU = transformer.transform(pt)!!
        assertASTsAreEqual(cu, transformedCU, considerRange = true)
        assertTrue { transformedCU.hasValidParents() }
        assertNull(transformedCU.invalidRanges().firstOrNull())
    }

    @Test
    fun testTransformationWithErrors() {
        val code = "set foo = \ndisplay @@@"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val pt = parser.compilationUnit()
        assertEquals(2, parser.numberOfSyntaxErrors)

        val transformer = ParseTreeToASTTransformer()
        configure(transformer)

        val cu =
            CU(
                statements =
                    listOf(
                        GenericErrorNode(message = "Exception java.lang.IllegalStateException: Parse error")
                            .withParseTreeNode(pt.statement(0)),
                        GenericErrorNode(message = "Exception java.lang.IllegalStateException: Parse error")
                            .withParseTreeNode(pt.statement(1)),
                    ),
            ).withParseTreeNode(pt)
        val transformedCU = transformer.transform(pt)!! as CU
        assertASTsAreEqual(cu, transformedCU, considerRange = true)
        assertTrue { transformedCU.hasValidParents() }
        assertNull(transformedCU.invalidRanges().firstOrNull())
    }

    @Test
    fun testGenericNode() {
        val code = "set foo = 123\ndisplay 456"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val pt = parser.compilationUnit()

        val transformer = ParseTreeToASTTransformer()
        assertASTsAreEqual(GenericNode(), transformer.transform(pt)!!)
    }

    @Test
    fun testGenericASTTransformer() {
        val code = "set foo = 123\ndisplay 456"
        val lexer = SimpleLangLexer(CharStreams.fromString(code))
        val parser = SimpleLangParser(CommonTokenStream(lexer))
        val pt = parser.compilationUnit()

        val transformer = ASTTransformer()
        configure(transformer)

        // Compared to ParseTreeToASTTransformer, the base class ASTTransformer does not assign a parse tree node
        // to each AST node
        val cu =
            CU(
                statements =
                    listOf(
                        SetStatement(variable = "foo", value = 123),
                        DisplayIntStatement(value = 456),
                    ),
            )
        val transformedCU = transformer.transform(pt)!!
        assertASTsAreEqual(cu, transformedCU, considerRange = true)
        assertTrue { transformedCU.hasValidParents() }
    }

    private fun configure(transformer: ASTTransformer) {
        transformer
            .registerNodeTransformer(SimpleLangParser.CompilationUnitContext::class, CU::class)
            .withChild(CU::statements, SimpleLangParser.CompilationUnitContext::statement)
        transformer.registerNodeTransformer(SimpleLangParser.DisplayStmtContext::class) { ctx ->
            if (ctx.exception != null || ctx.expression().exception != null) {
                // We throw a custom error so that we can check that it's recorded in the AST
                throw IllegalStateException("Parse error")
            }
            DisplayIntStatement(
                value =
                    ctx
                        .expression()
                        .INT_LIT()
                        .text
                        .toInt(),
            )
        }
        transformer.registerNodeTransformer(SimpleLangParser.SetStmtContext::class) { ctx ->
            if (ctx.exception != null || ctx.expression().exception != null) {
                // We throw a custom error so that we can check that it's recorded in the AST
                throw IllegalStateException("Parse error")
            }
            SetStatement(
                variable = ctx.ID().text,
                value =
                    ctx
                        .expression()
                        .INT_LIT()
                        .text
                        .toInt(),
            )
        }
    }

    class MyErrorListener : ANTLRErrorListener {
        override fun syntaxError(
            p0: Recognizer<*, *>?,
            p1: Any?,
            line: Int,
            column: Int,
            message: String?,
            p5: RecognitionException?,
        ): Unit = throw RuntimeException("L$line:$column: $message")

        override fun reportAmbiguity(
            p0: Parser?,
            p1: DFA?,
            p2: Int,
            p3: Int,
            p4: Boolean,
            p5: BitSet?,
            p6: ATNConfigSet?,
        ) {
            // nothing to do
        }

        override fun reportAttemptingFullContext(
            p0: Parser?,
            p1: DFA?,
            p2: Int,
            p3: Int,
            p4: BitSet?,
            p5: ATNConfigSet?,
        ) {
            // nothing to do
        }

        override fun reportContextSensitivity(
            p0: Parser?,
            p1: DFA?,
            p2: Int,
            p3: Int,
            p4: Int,
            p5: ATNConfigSet?,
        ) {
            // nothing to do
        }
    }

    private fun parseEntities(code: String): AntlrEntityParser.ModuleContext {
        val errorListener = MyErrorListener()

        val lexer = AntlrEntityLexer(CharStreams.fromString(code))
        lexer.removeErrorListeners()
        lexer.addErrorListener(errorListener)
        val parser = AntlrEntityParser(CommonTokenStream(lexer))
        parser.removeErrorListeners()
        parser.addErrorListener(errorListener)
        return parser.module()
    }

    private fun parseScript(code: String): AntlrScriptParser.ScriptContext {
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
        transformer.registerTrivialPTtoASTConversion<AntlrEntityParser.ModuleContext, EModule>()
        transformer.registerTrivialPTtoASTConversion<AntlrEntityParser.EntityContext, EEntity>()
        val expectedAST =
            EModule(
                "M",
                mutableListOf(
                    EEntity("FOO", mutableListOf()),
                    EEntity("BAR", mutableListOf()),
                ),
            )
        val actualAST =
            transformer.transform(
                parseEntities(
                    """
                    module M {
                       entity FOO { }
                       entity BAR { }
                    }
                    """.trimIndent(),
                ),
            )!!
        assertASTsAreEqual(expectedAST, actualAST)
    }

    @Test
    fun testEntitiesWithFeaturesTransformer() {
        val transformer = ParseTreeToASTTransformer(allowGenericNode = false)
        transformer.registerTrivialPTtoASTConversion<AntlrEntityParser.ModuleContext, EModule>()
        transformer.registerTrivialPTtoASTConversion<AntlrEntityParser.EntityContext, EEntity>()
        transformer.registerTrivialPTtoASTConversion<AntlrEntityParser.FeatureContext, EFeature>()
        transformer.registerTrivialPTtoASTConversion<AntlrEntityParser.String_typeContext, EStringType>()
        transformer.registerTrivialPTtoASTConversion<AntlrEntityParser.Boolean_typeContext, EBooleanType>()
        transformer.registerTrivialPTtoASTConversion<AntlrEntityParser.Entity_typeContext, EEntityRefType>(
            "target" to "entity",
        )
        val expectedAST =
            EModule(
                "M",
                mutableListOf(
                    EEntity(
                        "FOO",
                        mutableListOf(
                            EFeature("A", EStringType()),
                            EFeature("B", EBooleanType()),
                        ),
                    ),
                    EEntity(
                        "BAR",
                        mutableListOf(
                            EFeature("C", EEntityRefType(ReferenceValue("FOO"))),
                        ),
                    ),
                ),
            )
        val actualAST =
            transformer.transform(
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
                    """.trimIndent(),
                ),
            )!!
        assertASTsAreEqual(expectedAST, actualAST)
    }

    @Test
    fun testScriptTransformer() {
        val transformer = ParseTreeToASTTransformer(allowGenericNode = false)
        transformer.registerTrivialPTtoASTConversion<AntlrScriptParser.ScriptContext, SScript>()
        transformer.registerTrivialPTtoASTConversion<AntlrScriptParser.Create_statementContext, SCreateStatement>(
            AntlrScriptParser.Create_statementContext::var_name to SCreateStatement::name,
        )
        transformer.registerTrivialPTtoASTConversion<AntlrScriptParser.Set_statementContext, SSetStatement>()
        transformer.registerTrivialPTtoASTConversion<AntlrScriptParser.Entity_by_id_expressionContext, SInstanceById>(
            AntlrScriptParser.Entity_by_id_expressionContext::id to SInstanceById::index,
        )
        transformer.registerTrivialPTtoASTConversion<AntlrScriptParser.Int_literal_expressionContext, SIntegerLiteral>(
            AntlrScriptParser.Int_literal_expressionContext::INT_VALUE to SIntegerLiteral::value,
        )
        transformer.registerNodeTransformer(AntlrScriptParser.String_literal_expressionContext::class) { pt, t ->
            SStringLiteral(pt.text.removePrefix("'").removeSuffix("'"))
        }
        transformer.registerNodeTransformer(AntlrScriptParser.Div_mult_expressionContext::class) { pt, t ->
            when (pt.op.text) {
                "/" -> {
                    TrivialFactoryOfParseTreeToASTNodeTransformer.trivialTransformer<
                        AntlrScriptParser.Div_mult_expressionContext,
                        SDivision,
                        >()(pt, t)
                }

                "*" -> {
                    TrivialFactoryOfParseTreeToASTNodeTransformer.trivialTransformer<
                        AntlrScriptParser.Div_mult_expressionContext,
                        SMultiplication,
                        >()(pt, t)
                }

                else -> TODO()
            }
        }
        transformer.registerNodeTransformer(AntlrScriptParser.Sum_sub_expressionContext::class) { pt, t ->
            when (pt.op.text) {
                "+" -> {
                    TrivialFactoryOfParseTreeToASTNodeTransformer.trivialTransformer<
                        AntlrScriptParser.Sum_sub_expressionContext,
                        SSum,
                        >()(pt, t)
                }

                "-" -> {
                    TrivialFactoryOfParseTreeToASTNodeTransformer.trivialTransformer<
                        AntlrScriptParser.Sum_sub_expressionContext,
                        SSubtraction,
                        >()(pt, t)
                }

                else -> TODO()
            }
        }
        transformer.unwrap<AntlrScriptParser.Parens_expressionContext, SExpression>(
            AntlrScriptParser.Parens_expressionContext::expression,
        )
        transformer.registerTrivialPTtoASTConversion<AntlrScriptParser.Print_statementContext, SPrintStatement>()
        transformer.registerTrivialPTtoASTConversion<AntlrScriptParser.Concat_expressionContext, SConcat>()
        transformer.registerTrivialPTtoASTConversion<
            AntlrScriptParser.Feature_access_expressionContext,
            SFeatureAccess,
            >(
            AntlrScriptParser.Feature_access_expressionContext::instance to SFeatureAccess::container,
        )
        val expectedAST =
            SScript(
                mutableListOf(
                    SCreateStatement(ReferenceValue("Client")),
                    SSetStatement(
                        ReferenceValue("name"),
                        SInstanceById(
                            ReferenceValue("Client"),
                            SIntegerLiteral(1),
                        ),
                        SStringLiteral("ACME Inc."),
                    ),
                    SCreateStatement(ReferenceValue("Product")),
                    SSetStatement(
                        ReferenceValue("value"),
                        SInstanceById(
                            ReferenceValue("Product"),
                            SIntegerLiteral(2),
                        ),
                        SDivision(SSum(SIntegerLiteral(1500), SIntegerLiteral(200)), SIntegerLiteral(2)),
                    ),
                    SPrintStatement(
                        SConcat(
                            SStringLiteral("Value of Product #2 is: "),
                            SFeatureAccess(
                                ReferenceValue("value"),
                                SInstanceById(
                                    ReferenceValue("Product"),
                                    SIntegerLiteral(2),
                                ),
                            ),
                        ),
                    ),
                ),
            )
        val actualAST =
            transformer.transform(
                parseScript(
                    """
                    create Client
                    set name of Client #1 to 'ACME Inc.'
                    create Product
                    set value of Product #2 to (1500 + 200) / 2
                    print concat 'Value of Product #2 is: ' and value of Product #2
                    """.trimIndent(),
                ),
            )!!
        actualAST.walk().forEach { parent ->
            parent.children.forEach { child ->
                assertSame(parent, child.parent)
            }
        }
        assertASTsAreEqual(expectedAST, actualAST)
    }

    // Ensure that https://github.com/Strumenta/kolasu/issues/241 is fixed
    @Test
    fun transformChildFactory() {
        val ctx = EntCtx("foo", listOf(EntCtxFeature("bar", EntCtxStringType())))
        val transformer = EntTransformer()
        val ast = transformer.transform(ctx)
        assertASTsAreEqual(Ent("foo", listOf(EntFeature("bar", EntStringType()))), ast!!)
        assertASTsAreEqual(
            Ent("foo", listOf(EntFeature(null, null))),
            transformer.transform(EntCtx("foo", listOf(null, EntCtxFeature(), null)))!!,
        )
    }
}

class EntTransformer(
    issues: MutableList<Issue> = mutableListOf(),
) : ParseTreeToASTTransformer(issues, allowGenericNode = false) {
    init {
        registerNodeTransformer(EntCtx::class) { ctx -> Ent(ctx.name) }
            .withChild(Ent::features2, EntCtx::features2)
        registerNodeTransformer(EntCtxFeature::class) { ctx -> EntFeature(name = ctx.name) }
            .withChild(EntFeature::type, EntCtxFeature::type)
        this.registerNodeTransformer(EntCtxStringType::class, EntStringType::class)
    }
}

data class EntCtx(
    val name: String,
    val features2: List<EntCtxFeature?>,
)

data class EntCtxFeature(
    val name: String? = null,
    var type: EntCtxType? = null,
)

open class EntCtxType

open class EntCtxPrimitiveType : EntCtxType()

class EntCtxStringType : EntCtxPrimitiveType()

data class Ent(
    override val name: String,
    var features2: List<EntFeature> = listOf(),
) : Node(),
    Named

data class EntFeature(
    override val name: String? = null,
    var type: EntType? = null,
) : Node(),
    PossiblyNamed

open class EntType : Node()

open class EntPrimitiveType : EntType()

class EntStringType : EntPrimitiveType()
