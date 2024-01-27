@file:OptIn(ExperimentalCompilerApi::class, ExperimentalCompilerApi::class)

package com.strumenta.kolasu.kcp

import com.tschuchort.compiletesting.CompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test
import java.io.File
import java.net.URLClassLoader
import kotlin.test.assertEquals

fun CompilationResultWithClassLoader.assertHasMessage(regex: Regex) {
    val messageLines = this.messages.lines()
    assert(messageLines.any { regex.matches(it) })
}

fun CompilationResultWithClassLoader.assertHasMessage(msg: String) {
    val messageLines = this.messages.lines()
    assert(messageLines.any { msg.equals(it) })
}

fun CompilationResultWithClassLoader.assertHasNotMessage(regex: Regex) {
    val messageLines = this.messages.lines()
    assert(messageLines.none { regex.matches(it) })
}

class IrPluginTest {
    @Test
    fun `IR plugin success`() {
        val result =
            compile(
                sourceFile =
                    SourceFile.kotlin(
                        "main.kt",
                        """
fun main() {
  println(debug())
}

fun debug() = "Hello, World!"
""",
                    ),
            )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `An AST Class`() {
        val result =
            compile(
                sourceFile =
                    SourceFile.kotlin(
                        "main.kt",
                        """
          import com.strumenta.kolasu.model.Node

    data class MyNode(var p1: Int) : Node()

fun main() {
  val n = MyNode(1)
  n.p1 = 2
}

""",
                    ),
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
        val result =
            compile(
                sourceFile =
                    SourceFile.kotlin(
                        "main.kt",
                        """
          import com.strumenta.kolasu.model.Node

    data class MyNode(val p1: Int, var p2: String) : Node()

fun main() {
  val n = MyNode(1, "foo")
  n.p2 = "bar"
}

""",
                    ),
            )
        result.assertHasMessage(
            Regex("i: file:///[a-zA-Z0-9/\\-.]*:[0-9]+:[0-9]+ AST class MyNode identified"),
        )
        result.assertHasMessage(
            Regex("w: file:///[a-zA-Z0-9/\\-.]*:[0-9]+:[0-9]+ Value param MyNode.p1 is not assignable"),
        )
        result.assertHasNotMessage(
            Regex("w: file:///[a-zA-Z0-9/\\-.]*:[0-9]+:[0-9]+ Value param MyNode.p2 is not assignable"),
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `make node really observable`() {
        val result =
            compile(
                sourceFile =
                    SourceFile.kotlin(
                        "main.kt",
                        """
          package mytest

          import com.strumenta.kolasu.model.Node
          import com.strumenta.kolasu.ast.NodeLike
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
        node: NodeLike,
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

""",
                    ),
            )
        result.assertHasMessage(Regex("i: file:///[a-zA-Z0-9/\\-.]*:6:6 AST class mytest.MyNode identified"))

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val mainKt = result.classLoader.loadClass("mytest.MainKt")
        mainKt.methods.find { it.name == "main" }!!.invoke(null)
        val myObserverClass = result.classLoader.loadClass("mytest.MyObserver")
        val myObserver = myObserverClass.fields.find { it.name == "INSTANCE" }!!.get(null)
        val observations =
            myObserverClass
                .methods
                .find {
                    it.name == "getObservations"
                }!!
                .invoke(myObserver) as List<String>
        assertEquals(listOf("p1: 1 -> 2", "p1: 2 -> 3", "p2: 0 -> 4"), observations)
    }

    @Test
    fun `auto set parent for single containment`() {
        val result =
            compile(
                sourceFile =
                    SourceFile.kotlin(
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

""",
                    ),
            )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        result.invokeMainMethod("mytest.MainKt")
    }

    @Test
    fun `force to use ObservableLists`() {
        val result =
            compile(
                sourceFile =
                    SourceFile.kotlin(
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

""",
                    ),
            )
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        result.assertHasMessage("e: AST Nodes should use ObservableLists (see mytest.MyNode.p4)")
    }

    @Test
    fun `auto set parent for multiple containment`() {
        val result =
            compile(
                sourceFile =
                    SourceFile.kotlin(
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

""",
                    ),
            )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        result.invokeMainMethod("mytest.MainKt")
    }

    @Test
    fun `references are auto-observed`() {
        val result =
            compile(
                sourceFile =
                    SourceFile.kotlin(
                        "main.kt",
                        """
          package mytest

          import com.strumenta.kolasu.ast.NodeLike
          import com.strumenta.kolasu.model.Node
          import com.strumenta.kolasu.model.observable.ObservableList
          import com.strumenta.kolasu.model.observable.MultiplePropertyListObserver
        import com.strumenta.kolasu.model.Named
        import com.strumenta.kolasu.model.ReferenceByName
        import kotlin.test.Test
        import kotlin.test.assertEquals
        import com.strumenta.kolasu.model.observable.SimpleNodeObserver


data class NamedNode(override val name: String) : Node(), Named

data class NodeWithReference(val ref: ReferenceByName<NamedNode>, val id: Int) : Node()

class MyObserver : SimpleNodeObserver() {
    val observations = mutableListOf<String>()
    override fun <V> onAttributeChange(node: NodeLike, attributeName: String, oldValue: V, newValue: V) {
        observations.add("${'$'}attributeName: ${'$'}oldValue -> ${'$'}newValue")
    }

    override fun onChildAdded(node: NodeLike, containmentName: String, added: NodeLike) {
        observations.add("${'$'}containmentName: added ${'$'}added")
    }

    override fun onChildRemoved(node: NodeLike, containmentName: String, removed: NodeLike) {
        observations.add("${'$'}containmentName: removed ${'$'}removed")
    }

    override fun onReferenceSet(node: NodeLike, referenceName: String, oldReferredNode: NodeLike?, newReferredNode: NodeLike?) {
        val oldName = if (oldReferredNode == null) "null" else (oldReferredNode as? Named)?.name ?: "<UNKNOWN>"
        val newName = if (newReferredNode == null) "null" else (newReferredNode as? Named)?.name ?: "<UNKNOWN>"
        observations.add("${'$'}referenceName: changed from ${'$'}oldName to ${'$'}newName")
    }

    override fun onReferringAdded(node: NodeLike, referenceName: String, referring: NodeLike) {
        val myName = (node as? Named)?.name ?: "<UNKNOWN>"
        observations.add("${'$'}myName is now referred to by ${'$'}referring.${'$'}referenceName")
    }

    override fun onReferringRemoved(node: NodeLike, referenceName: String, referring: NodeLike) {
        val myName = (node as? Named)?.name ?: "<UNKNOWN>"
        observations.add("${'$'}myName is not referred anymore by ${'$'}referring.${'$'}referenceName")
    }
}

fun main() {
       val obs1 = MyObserver()
        val obs2 = MyObserver()
        val obsA = MyObserver()
        val obsB = MyObserver()

        fun clearObservations() {
            obs1.observations.clear()
            obs2.observations.clear()
            obsA.observations.clear()
            obsB.observations.clear()
        }

        val nwr1 = NodeWithReference(ReferenceByName("foo"), 1)
        val nwr2 = NodeWithReference(ReferenceByName("bar"), 2)
        val a = NamedNode("a")
        val b = NamedNode("b")

        nwr1.subscribe(obs1)
        nwr2.subscribe(obs2)
        a.subscribe(obsA)
        b.subscribe(obsB)

        nwr1.ref.referred = a
        assertEquals(listOf("ref: changed from null to a"), obs1.observations)
        assertEquals(listOf(), obs2.observations)
        assertEquals(
            listOf(
                "a is now referred to by mytest." +
                    "NodeWithReference(id=1, ref=Ref(foo)[Unsolved]).ref"
            ),
            obsA.observations
        )
        assertEquals(listOf(), obsB.observations)
        clearObservations()

        nwr1.ref.referred = b
        assertEquals(listOf("ref: changed from a to b"), obs1.observations)
        assertEquals(listOf(), obs2.observations)
        assertEquals(
            listOf(
                "a is not referred anymore by mytest." +
                    "NodeWithReference(id=1, ref=Ref(foo)[Solved]).ref"
            ),
            obsA.observations
        )
        assertEquals(
            listOf(
                "b is now referred to by mytest." +
                    "NodeWithReference(id=1, ref=Ref(foo)[Solved]).ref"
            ),
            obsB.observations
        )
        clearObservations()

        nwr1.ref.referred = null
        assertEquals(listOf("ref: changed from b to null"), obs1.observations)
        assertEquals(listOf(), obs2.observations)
        assertEquals(listOf(), obsA.observations)
        assertEquals(
            listOf(
                "b is not referred anymore by mytest." +
                    "NodeWithReference(id=1, ref=Ref(foo)[Solved]).ref"
            ),
            obsB.observations
        )
        clearObservations()

        nwr2.ref.referred = a
        assertEquals(listOf(), obs1.observations)
        assertEquals(listOf("ref: changed from null to a"), obs2.observations)
        assertEquals(
            listOf(
                "a is now referred to by mytest." +
                    "NodeWithReference(id=2, ref=Ref(bar)[Unsolved]).ref"
            ),
            obsA.observations
        )
        assertEquals(listOf(), obsB.observations)
}

""",
                    ),
            )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        result.invokeMainMethod("mytest.MainKt")
    }

    private fun CompilationResultWithClassLoader.invokeMainMethod(className: String) {
        val mainKt = this.classLoader.loadClass(className)
        val mainMethod =
            mainKt.methods.find { it.name == "main" }
                ?: throw IllegalArgumentException("Main method not found in compiled code")
        when (mainMethod.parameterCount) {
            0 -> {
                mainMethod.invoke(null)
            }

            1 -> {
                mainMethod.invoke(null, arrayOf<String>())
            }

            else -> {
                throw IllegalStateException(
                    "The main method found expect these parameters: ${mainMethod.parameters}. " +
                        "Main method: $mainMethod",
                )
            }
        }
    }
}

@ExperimentalCompilerApi
fun compile(
    sourceFiles: List<SourceFile>,
    plugin: CompilerPluginRegistrar = StarLasuComponentRegistrar(),
): CompilationResultWithClassLoader {
    val kotlinCompilation =
        KotlinCompilation().apply {
            sources = sourceFiles
            // useIR = true
            compilerPluginRegistrars = listOf(plugin)
            inheritClassPath = true
        }
    val result = kotlinCompilation.compile()
    return CompilationResultWithClassLoader(result, kotlinCompilation.classpaths, kotlinCompilation.classesDir)
}

data class CompilationResultWithClassLoader(
    val compilationResult: CompilationResult,
    val classpaths: kotlin.collections.List<java.io.File>,
    val outputDirectory: File,
) {
    val messages: String
        get() = compilationResult.messages
    val exitCode: KotlinCompilation.ExitCode
        get() = compilationResult.exitCode

    // It is important to REUSE the classloader and not re-create it
    val classLoader =
        URLClassLoader(
            // Include the original classpaths and the output directory to be able to load classes from dependencies.
            classpaths.plus(outputDirectory).map { it.toURI().toURL() }.toTypedArray(),
            this::class.java.classLoader,
        )
}

fun compile(
    sourceFile: SourceFile,
    plugin: CompilerPluginRegistrar = StarLasuComponentRegistrar(),
): CompilationResultWithClassLoader = compile(listOf(sourceFile), plugin)
