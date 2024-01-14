package com.strumenta.kolasu.codegen

import com.strumenta.kolasu.model.INode
import com.strumenta.kolasu.model.ReferenceByName
import org.junit.Test
import kotlin.test.assertEquals

class ASTCodeGeneratorTest {
    @Test
    fun printSimpleKotlinExpression() {
        val ex =
            KMethodCallExpression(
                KThisExpression(),
                ReferenceByName("myMethod"),
                mutableListOf(KStringLiteral("abc"), KIntLiteral(123), KStringLiteral("qer")),
            )
        val code = KotlinPrinter().printToString(ex)
        assertEquals("""this.myMethod("abc", 123, "qer")""", code)
    }

    @Test
    fun printSimpleFile() {
        val cu =
            KCompilationUnit(
                KPackageDecl("my.splendid.packag"),
                mutableListOf(KImport("my.imported.stuff")),
                mutableListOf(KFunctionDeclaration("foo")),
            )
        val code = KotlinPrinter().printToString(cu)
        assertEquals(
            """package my.splendid.packag
            |
            |import my.imported.stuff
            |
            |
            |fun foo() {
            |}
            |
            """.trimMargin(),
            code,
        )
    }

    @Test
    fun printUsingNodePrinterOverrider() {
        val ex =
            KMethodCallExpression(
                KThisExpression(),
                ReferenceByName("myMethod"),
                mutableListOf(KStringLiteral("abc"), KIntLiteral(123), KStringLiteral("qer")),
            )
        val code = KotlinPrinter().printToString(ex)
        assertEquals("""this.myMethod("abc", 123, "qer")""", code)

        val codeWithNodePrinterOverrider =
            KotlinPrinter()
                .also {
                    it.nodePrinterOverrider = { n: INode ->
                        when (n) {
                            is KStringLiteral -> NodePrinter { output, ast -> output.print("YYY") }
                            is KIntLiteral -> NodePrinter { output, ast -> output.print("XXX") }
                            else -> null
                        }
                    }
                }.printToString(ex)
        assertEquals("""this.myMethod(YYY, XXX, YYY)""", codeWithNodePrinterOverrider)
    }
}
