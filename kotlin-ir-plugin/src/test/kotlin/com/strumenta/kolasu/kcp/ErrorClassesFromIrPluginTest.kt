@file:OptIn(ExperimentalCompilerApi::class)

package com.strumenta.kolasu.kcp

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test
import kotlin.test.assertEquals

class ErrorClassesFromIrPluginTest : AbstractIrPluginTest() {

    @Test
    fun `An Error Class is generated`() {
        val result =
            compile(
                sourceFile =
                    SourceFile.kotlin(
                        "main.kt",
                        """
          import com.strumenta.kolasu.model.MPNode
          import com.strumenta.kolasu.language.StarLasuLanguage
          import kotlin.test.assertEquals

    object LanguageMyTest : StarLasuLanguage("mytest")

    data class MyNode(var p1: Int) : MPNode()

fun main() {
  val n = MyNode.Error("I could not create MyNode")
  assertEquals(true, n is MyNode)  
  assertEquals("MyNode", n.concept.name)
}

""",
                    ),
            )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }


}
