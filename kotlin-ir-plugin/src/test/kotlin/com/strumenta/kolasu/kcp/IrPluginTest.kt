@file:OptIn(ExperimentalCompilerApi::class)

package com.strumenta.kolasu.kcp

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test
import kotlin.test.assertEquals

fun KotlinCompilation.Result.assertHasMessage(regex: Regex) {
    val messageLines = this.messages.lines()
    assert(messageLines.any { regex.matches(it) })
}

fun KotlinCompilation.Result.assertHasMessage(msg: String) {
    val messageLines = this.messages.lines()
    assert(messageLines.any { msg.equals(it) })
}

fun KotlinCompilation.Result.assertHasNotMessage(regex: Regex) {
    val messageLines = this.messages.lines()
    assert(messageLines.none { regex.matches(it) })
}

class IrPluginTest {
    @Test
    fun `IR plugin success`() {
        val result = compile(
            sourceFile = SourceFile.kotlin(
                "main.kt",
                """
fun main() {
  println(debug())
}

fun debug() = "Hello, World!"
"""
            )
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `An AST Class`() {
        val result = compile(
            sourceFile = SourceFile.kotlin(
                "main.kt",
                """
          import com.strumenta.kolasu.model.Node

    data class MyNode(var p1: Int) : Node()

fun main() {
  val n = MyNode(1)
  n.p1 = 2
}

"""
            )
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun regexForTmpFiles() {
        val msg =
            "i: file:///var/folders/lf/mnglpnp95kg1dj4p2tfzsh6c0000gp/T/" +
                "Kotlin-Compilation3641877337776772550/sources/main.kt:2:2 AST class MyNode identified"
        assert(Regex("i: file:///[a-zA-Z0-9/\\-.]*:2:2 AST class MyNode identified").matches(msg))
    }

    @Test
    fun `An AST Class should have var properties`() {
        val result = compile(
            sourceFile = SourceFile.kotlin(
                "main.kt",
                """
          import com.strumenta.kolasu.model.Node

    data class MyNode(val p1: Int, var p2: String) : Node()

fun main() {
  val n = MyNode(1, "foo")
  n.p2 = "bar"
}

"""
            )
        )
        result.assertHasMessage(
            Regex("i: file:///[a-zA-Z0-9/\\-.]*:[0-9]+:[0-9]+ AST class MyNode identified")
        )
        result.assertHasMessage(
            Regex("w: file:///[a-zA-Z0-9/\\-.]*:[0-9]+:[0-9]+ Value param MyNode.p1 is not assignable")
        )
        result.assertHasNotMessage(
            Regex("w: file:///[a-zA-Z0-9/\\-.]*:[0-9]+:[0-9]+ Value param MyNode.p2 is not assignable")
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `make node really observable`() {
        val result = compile(
            sourceFile = SourceFile.kotlin(
                "main.kt",
                """
          package mytest

          import com.strumenta.kolasu.model.Node
          import com.strumenta.kolasu.model.observable.SimpleNodeObserver

    data class MyNode(var p1: Int) : Node()

class Foo : Node() {
var p2 : Int = 0 
    set(value) {
        notifyOfPropertyChange("p2", field, value)
        field = value
    }
}

object MyObserver : SimpleNodeObserver() {
    val observations = mutableListOf<String>()
    override fun <V : Any?>onAttributeChange(
        node: Node,
        attributeName: String,
        oldValue: V,
        newValue: V
    ) {
        observations.add("${'$'}attributeName: ${'$'}oldValue -> ${'$'}newValue")
    }

}

fun main() {
  val n = MyNode(1)
  n.subscribe(MyObserver)
  n.p1 = 2
  n.p1 = 3
  val f = Foo()
  f.subscribe(MyObserver)
  f.p2 = 4
}

"""
            )
        )
        result.assertHasMessage(Regex("i: file:///[a-zA-Z0-9/\\-.]*:5:5 AST class mytest.MyNode identified"))

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val mainKt = result.classLoader.loadClass("mytest.MainKt")
        mainKt.methods.find { it.name == "main" }!!.invoke(null)
        val myObserverClass = result.classLoader.loadClass("mytest.MyObserver")
        val myObserver = myObserverClass.fields.find { it.name == "INSTANCE" }!!.get(null)
        val observations = myObserverClass.methods.find {
            it.name == "getObservations"
        }!!.invoke(myObserver) as List<String>
        assertEquals(listOf("p1: 1 -> 2", "p1: 2 -> 3", "p2: 0 -> 4"), observations)
    }

    @Test
    fun `auto set parent for single containment`() {
        val result = compile(
            sourceFile = SourceFile.kotlin(
                "main.kt",
                """
          package mytest

          import com.strumenta.kolasu.model.Node

    data class MyNode(var p1: MyNode? = null) : Node()

fun main() {
  val n1 = MyNode()
  val n2 = MyNode()
  require(n1.parent == null) { "n1.parent is not initially null" }
  require(n2.parent == null) { "n2.parent is not initially null" }
  n1.p1 = n2
  require(n1.parent == null) { "n1.parent does not remain null" }
  require(n2.parent == n1) { "n2.parent does not change from null" }
}

"""
            )
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val mainKt = result.classLoader.loadClass("mytest.MainKt")
        mainKt.methods.find { it.name == "main" }!!.invoke(null)
    }

    @Test
    fun `force to use ObservableLists`() {
        val result = compile(
            sourceFile = SourceFile.kotlin(
                "main.kt",
                """
          package mytest

          import com.strumenta.kolasu.model.Node

    data class MyNode(var p4: MutableList<MyNode> = mutableListOf()) : Node()

fun main() {
  val n1 = MyNode()
  val n2 = MyNode()
  val n3 = MyNode()
  require(n1.parent == null) { "n1.parent is not initially null" }
  require(n2.parent == null) { "n2.parent is not initially null" }
  require(n3.parent == null) { "n3.parent is not initially null" }
  n1.p4.add(n2)
  require(n1.parent == null) { "n1.parent does not remain null" }
  require(n2.parent == n1) { "n2.parent does not change from null" }
  require(n3.parent == null) { "n3.parent does not remain null" }
  n1.p4.add(n3)
  require(n1.parent == null) { "n1.parent does not remain null" }
  require(n2.parent == n1) { "n2.parent does not change from null" }
  require(n3.parent == n1) { "n3.parent does not change from null" }
}

"""
            )
        )
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        result.assertHasMessage("e: AST Nodes should use ObservableLists (see mytest.MyNode.p4)")
    }

    @Test
    fun `auto set parent for multiple containment`() {
        val result = compile(
            sourceFile = SourceFile.kotlin(
                "main.kt",
                """
          package mytest

          import com.strumenta.kolasu.model.Node
          import com.strumenta.kolasu.model.observable.ObservableList
          import com.strumenta.kolasu.model.observable.MultiplePropertyListObserver

    data class MyNode(var p4: ObservableList<MyNode> = ObservableList()) : Node()

fun main() {
  val n1 = MyNode()
  val n2 = MyNode()
  val n3 = MyNode()
  require(n1.parent == null) { "n1.parent is not initially null" }
  require(n2.parent == null) { "n2.parent is not initially null" }
  require(n3.parent == null) { "n3.parent is not initially null" }
  n1.p4.add(n2)
  require(n1.parent == null) { "n1.parent does not remain null" }
  require(n2.parent == n1) { "n2.parent does not change from null" }
  require(n3.parent == null) { "n3.parent does not remain null" }
  n1.p4.add(n3)
  require(n1.parent == null) { "n1.parent does not remain null" }
  require(n2.parent == n1) { "n2.parent does not change from null" }
  require(n3.parent == n1) { "n3.parent does not change from null" }
}

"""
            )
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val mainKt = result.classLoader.loadClass("mytest.MainKt")
        mainKt.methods.find { it.name == "main" }!!.invoke(null)
    }
}

fun compile(
    sourceFiles: List<SourceFile>,
    plugin: CompilerPluginRegistrar = StarLasuComponentRegistrar()
): KotlinCompilation.Result {
    return KotlinCompilation().apply {
        sources = sourceFiles
        useIR = true
        compilerPluginRegistrars = listOf(plugin)
        inheritClassPath = true
    }.compile()
}

fun compile(
    sourceFile: SourceFile,
    plugin: CompilerPluginRegistrar = StarLasuComponentRegistrar()
): KotlinCompilation.Result {
    return compile(listOf(sourceFile), plugin)
}
