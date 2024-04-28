@file:OptIn(ExperimentalCompilerApi::class, ExperimentalCompilerApi::class)

package com.strumenta.kolasu.kcp

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test
import kotlin.test.assertEquals

class IrPluginStarLasuLanguageTest : AbstractIrPluginTest() {
    @Test
    fun `get language instance directly`() {
        val result =
            compile(
                sourceFile =
                    SourceFile.kotlin(
                        "main.kt",
                        """
package mytest

import com.strumenta.kolasu.language.Concept
import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.model.MPNode
import kotlin.test.Test
import kotlin.test.assertEquals

data class SimpleNode(var foo: String, var other: SimpleNode? = null) : MPNode()

object LanguageMyTest : StarLasuLanguage("mytest")

fun main() {
    val starLasuLanguageInstance = LanguageMyTest
    assertEquals("mytest", starLasuLanguageInstance.qualifiedName)
    assertEquals("mytest", starLasuLanguageInstance.simpleName)
    assertEquals(1, starLasuLanguageInstance.types.size)
    val concept = starLasuLanguageInstance.types[0] as Concept
    assertEquals("SimpleNode", concept.name)
}
""",
                    ),
            )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        result.invokeMainMethod("mytest.MainKt")
    }

    @Test
    fun `get language instance through concept`() {
        val result =
            compile(
                sourceFile =
                    SourceFile.kotlin(
                        "main.kt",
                        """
package mytest

import com.strumenta.kolasu.language.Concept
import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.model.MPNode
import kotlin.test.Test
import kotlin.test.assertEquals

data class SimpleNode(var foo: String, var other: SimpleNode? = null) : MPNode()

object LanguageMyTest : StarLasuLanguage("mytest")

fun main() {
    val starLasuLanguageInstance = SimpleNode.concept.language
    assertEquals("mytest", starLasuLanguageInstance.qualifiedName)
    assertEquals("mytest", starLasuLanguageInstance.simpleName)
    assertEquals(1, starLasuLanguageInstance.types.size)
    val concept = starLasuLanguageInstance.types[0] as Concept
    assertEquals("SimpleNode", concept.name)
}

""",
                    ),
            )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        result.invokeMainMethod("mytest.MainKt")
    }
}
