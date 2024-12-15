@file:OptIn(ExperimentalCompilerApi::class)

package com.strumenta.kolasu.kcp

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class IrPluginTest : AbstractIrPluginTest() {
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
          import com.strumenta.kolasu.model.MPNode
          import com.strumenta.kolasu.language.StarLasuLanguage

    object LanguageMyTest : StarLasuLanguage("mytest")

    data class MyNode(var p1: Int) : MPNode()

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
          import com.strumenta.kolasu.model.MPNode
import com.strumenta.kolasu.language.StarLasuLanguage

    object LanguageMyTest : StarLasuLanguage("mytest")

    data class MyNode(val p1: Int, var p2: String) : MPNode()

fun main() {
  val n = MyNode(1, "foo")
  n.p2 = "bar"
}

""",
                    ),
            )
        result.assertHasMessage(
            Regex("i: file:///[a-zA-Z0-9/\\-._]*:[0-9]+:[0-9]+ MPNode class MyNode identified"),
        )
        result.assertHasMessage(
            Regex("w: file:///[a-zA-Z0-9/\\-._]*:[0-9]+:[0-9]+ Value param MyNode.p1 is not assignable"),
        )
        result.assertHasNotMessage(
            Regex("w: file:///[a-zA-Z0-9/\\-._]*:[0-9]+:[0-9]+ Value param MyNode.p2 is not assignable"),
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

          import com.strumenta.kolasu.model.MPNode
          import com.strumenta.kolasu.language.Property
          import com.strumenta.kolasu.model.NodeLike
          import com.strumenta.kolasu.model.observable.SimpleNodeObserver

import com.strumenta.kolasu.language.StarLasuLanguage

    object LanguageMyTest : StarLasuLanguage("mytest")
    data class MyNode(var p1: Int) : MPNode()

class Foo : MPNode() {
var p2 : Int = 0 
    set(value) {
        notifyOfPropertyChange(Foo.concept.property("p2")!!, field, value)
        field = value
    }
}

object MyObserver : SimpleNodeObserver() {
    val observations = mutableListOf<String>()
    override fun <V : Any?>onPropertyChange(
        node: NodeLike,
        property: Property,
        oldValue: V,
        newValue: V
    ) {
        observations.add("${'$'}{property.name}: ${'$'}oldValue -> ${'$'}newValue")
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
        result.assertHasMessage(Regex("i: file:///[a-zA-Z0-9/\\-._]*:10:10 MPNode class mytest.MyNode identified"))

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
import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.model.MPNode

object LanguageMyTest : StarLasuLanguage("mytest")
data class MyNode(var p1: MyNode? = null) : MPNode()

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

          import com.strumenta.kolasu.model.MPNode
          import com.strumenta.kolasu.model.observable.ObservableList
          import com.strumenta.kolasu.model.observable.MultiplePropertyListObserver
import com.strumenta.kolasu.language.StarLasuLanguage

object LanguageMyTest : StarLasuLanguage("mytest")
    data class MyNode(var p4: ObservableList<MyNode> = ObservableList()) : MPNode()

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

        import com.strumenta.kolasu.language.Property
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.Reference
          import com.strumenta.kolasu.model.NodeLike
          import com.strumenta.kolasu.model.Node
          import com.strumenta.kolasu.model.observable.ObservableList
          import com.strumenta.kolasu.model.observable.MultiplePropertyListObserver
        import com.strumenta.kolasu.model.Named
        import com.strumenta.kolasu.model.ReferenceValue
        import kotlin.test.Test
        import kotlin.test.assertEquals
        import com.strumenta.kolasu.model.observable.SimpleNodeObserver
import com.strumenta.kolasu.language.explore
import com.strumenta.kolasu.language.StarLasuLanguage

object StarLasuLanguageInstance : StarLasuLanguage("mytest") {
    init {
        explore(NamedNode::class, NodeWithReference::class)
    }
}

data class NamedNode(override val name: String) : Node(), Named

data class NodeWithReference(val ref: ReferenceValue<NamedNode>, val id: Int) : Node()

class MyObserver : SimpleNodeObserver() {
    val observations = mutableListOf<String>()
    override fun <V> onPropertyChange(node: NodeLike, property: Property, oldValue: V, newValue: V) {
        observations.add("${'$'}{property.name}: ${'$'}oldValue -> ${'$'}newValue")
    }

    override fun onChildAdded(node: NodeLike, containment: Containment, added: NodeLike) {
        observations.add("${'$'}{containment.name}: added ${'$'}added")
    }

    override fun onChildRemoved(node: NodeLike, containment: Containment, removed: NodeLike) {
        observations.add("${'$'}{containment.name}: removed ${'$'}removed")
    }

    override fun onReferenceSet(node: NodeLike, reference: Reference, oldReferredNode: NodeLike?, newReferredNode: NodeLike?) {
        val oldName = if (oldReferredNode == null) "null" else (oldReferredNode as? Named)?.name ?: "<UNKNOWN>"
        val newName = if (newReferredNode == null) "null" else (newReferredNode as? Named)?.name ?: "<UNKNOWN>"
        observations.add("${'$'}{reference.name}: changed from ${'$'}oldName to ${'$'}newName")
    }

    override fun onReferringAdded(node: NodeLike, reference: Reference, referring: NodeLike) {
        val myName = (node as? Named)?.name ?: "<UNKNOWN>"
        observations.add("${'$'}myName is now referred to by ${'$'}referring.${'$'}{reference.name}")
    }

    override fun onReferringRemoved(node: NodeLike, reference: Reference, referring: NodeLike) {
        val myName = (node as? Named)?.name ?: "<UNKNOWN>"
        observations.add("${'$'}myName is not referred anymore by ${'$'}referring.${'$'}{reference.name}")
    }
}

fun main() {
    StarLasuLanguageInstance.ensureIsRegistered()
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

        val nwr1 = NodeWithReference(ReferenceValue("foo"), 1)
        val nwr2 = NodeWithReference(ReferenceValue("bar"), 2)
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

    @Test
    fun `check features are listed correctly`() {
        val result =
            compile(
                sourceFile =
                    SourceFile.kotlin(
                        "main.kt",
                        """
package mytest

import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.MPNode
import com.strumenta.kolasu.language.Property
import com.strumenta.kolasu.language.PrimitiveType
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.model.FeatureType
import com.strumenta.kolasu.model.Multiplicity
import com.strumenta.kolasu.model.observable.ObservableList
import com.strumenta.kolasu.model.observable.MultiplePropertyListObserver
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.ReferenceValue
import kotlin.test.Test
import kotlin.test.assertEquals
import com.strumenta.kolasu.model.observable.SimpleNodeObserver
import com.strumenta.kolasu.language.StarLasuLanguage

object LanguageMyTest : StarLasuLanguage("mytest")

data class A(val f1: String, val f2: Int, val f3: A? = null) : MPNode()

fun main() {
    val a1 = A("Foo", 6)
    val a2 = A("Bar", 18, a1)
    val a3 = A("Zum", 99, a2)

    assertEquals(3, a1.concept.declaredFeatures.size)
    
    assertEquals("f1", a1.concept.declaredFeatures[0].name)
    assertEquals("Foo", a1.concept.declaredFeatures[0].value(a1))
    assertEquals(true, a1.concept.declaredFeatures[0] is Property)
    assertEquals(PrimitiveType("kotlin.String"), a1.concept.declaredFeatures[0].type)
    assertEquals(Multiplicity.SINGULAR, a1.concept.declaredFeatures[0].multiplicity)
    assertEquals(false, a1.concept.declaredFeatures[0].derived)

    assertEquals("f2", a1.concept.declaredFeatures[1].name)
    assertEquals(6, a1.concept.declaredFeatures[1].value(a1))
    assertEquals(true, a1.concept.declaredFeatures[1] is Property)
    assertEquals(PrimitiveType("kotlin.Int"), a1.concept.declaredFeatures[1].type)
    assertEquals(Multiplicity.SINGULAR, a1.concept.declaredFeatures[1].multiplicity)
    assertEquals(false, a1.concept.declaredFeatures[1].derived)
    
    assertEquals("f3", a1.concept.declaredFeatures[2].name)
    assertEquals(null, a1.concept.declaredFeatures[2].value(a1))
    assertEquals(true, a1.concept.declaredFeatures[2] is Containment)
    assertEquals(a1.concept, a1.concept.declaredFeatures[2].type)
    assertEquals(Multiplicity.OPTIONAL, a1.concept.declaredFeatures[2].multiplicity)
    assertEquals(false, a1.concept.declaredFeatures[2].derived)
}

""",
                    ),
            )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        result.invokeMainMethod("mytest.MainKt")
    }

    @Test
    fun `node type is overridden also for subclasses`() {
        val result =
            compile(
                sourceFile =
                    SourceFile.kotlin(
                        "main.kt",
                        """
package mytest

import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.MPNode
import com.strumenta.kolasu.model.FeatureType
import com.strumenta.kolasu.model.Multiplicity
import com.strumenta.kolasu.model.observable.ObservableList
import com.strumenta.kolasu.model.observable.MultiplePropertyListObserver
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.ReferenceValue
import kotlin.test.Test
import kotlin.test.assertEquals
import com.strumenta.kolasu.model.observable.SimpleNodeObserver
import com.strumenta.kolasu.language.StarLasuLanguage

object LanguageMyTest : StarLasuLanguage("mytest")

sealed class Expression : MPNode()

sealed class Statement : MPNode()

class InputDeclaration : Statement()

data class SumExpr(var left: Expression, var right: Expression) : Expression()

data class IntLiteral(var value: Int) : Expression()

class VarDeclaration(override var name: String, var value: Expression) : Statement(), Named

class MiniCalcFile(val statements : MutableList<Statement>) : MPNode()

fun main() {
    val id = InputDeclaration()
    val se = SumExpr(IntLiteral(1), IntLiteral(2))
    val il = IntLiteral(3)
    val vd = VarDeclaration("Foo", IntLiteral(4))
    val mv = MiniCalcFile(mutableListOf())

    assertEquals("MiniCalcFile", mv.nodeType)
    assertEquals("InputDeclaration", id.nodeType)
    assertEquals("SumExpr", se.nodeType)
    assertEquals("IntLiteral", il.nodeType)
    assertEquals("VarDeclaration", vd.nodeType) 
}

""",
                    ),
            )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        result.invokeMainMethod("mytest.MainKt")
    }

    @Test
    fun `get concept on class`() {
        val result =
            compile(
                sourceFile =
                    SourceFile.kotlin(
                        "main.kt",
                        """
package mytest

import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.MPNode
import com.strumenta.kolasu.model.FeatureType
import com.strumenta.kolasu.model.Multiplicity
import com.strumenta.kolasu.model.observable.ObservableList
import com.strumenta.kolasu.model.observable.MultiplePropertyListObserver
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.ReferenceValue
import kotlin.test.Test
import kotlin.test.assertEquals
import com.strumenta.kolasu.model.observable.SimpleNodeObserver
import com.strumenta.kolasu.language.StarLasuLanguage

object LanguageMyTest : StarLasuLanguage("mytest")
data class SimpleNode(var foo: String, var other: SimpleNode? = null) : MPNode()

fun main() {
    assertEquals("SimpleNode", SimpleNode.concept.name)
}

""",
                    ),
            )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        result.invokeMainMethod("mytest.MainKt")
    }

    @Test
    fun `get concept on instance`() {
        val result =
            compile(
                sourceFile =
                    SourceFile.kotlin(
                        "main.kt",
                        """
package mytest

import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.MPNode
import com.strumenta.kolasu.model.FeatureType
import com.strumenta.kolasu.model.Multiplicity
import com.strumenta.kolasu.model.observable.ObservableList
import com.strumenta.kolasu.model.observable.MultiplePropertyListObserver
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.ReferenceValue
import kotlin.test.Test
import kotlin.test.assertEquals
import com.strumenta.kolasu.model.observable.SimpleNodeObserver
import com.strumenta.kolasu.language.StarLasuLanguage

object LanguageMyTest : StarLasuLanguage("mytest")

data class SimpleNode(var foo: String, var other: SimpleNode? = null) : MPNode()

fun main() {
    val n1 = SimpleNode("a")

    assertEquals("SimpleNode", n1.concept.name)
}

""",
                    ),
            )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        result.invokeMainMethod("mytest.MainKt")
    }

    @Test
    fun `KolasuGen Annotation`() {
        val result =
            compile(
                sourceFile =
                    SourceFile.kotlin(
                        "main.kt",
                        """
          import com.strumenta.kolasu.model.MPNode
          import com.strumenta.kolasu.model.KolasuGen
          import com.strumenta.kolasu.language.StarLasuLanguage

    object LanguageMyTest : StarLasuLanguage("mytest")

    @KolasuGen
    data class MyNode(var p1: Int) : MPNode()

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
    fun `Enum in Language`() {
        val result =
            compile(
                sourceFile =
                    SourceFile.kotlin(
                        "main.kt",
                        """
package mytest

          import com.strumenta.kolasu.model.MPNode
          import com.strumenta.kolasu.model.KolasuGen
          import com.strumenta.kolasu.language.Property
          import com.strumenta.kolasu.language.EnumType
          import com.strumenta.kolasu.language.StarLasuLanguage
          import kotlin.test.assertNotNull
import kotlin.test.assertEquals

    object LanguageMyTest : StarLasuLanguage("mytest")

    @KolasuGen
    enum class MyEnum { A, B}

    @KolasuGen
    data class MyNode(var p1: MyEnum) : MPNode()

fun main() {
  val n = MyNode(MyEnum.A)
  n.p1 = MyEnum.B
  val c = n.concept
  val p = c.declaredFeatures.find { it.name == "p1" } as Property
  assert(p.type is EnumType)
  val l = c.language
  val e = l.getEnum("MyEnum")
  assertNotNull(e)
  assertEquals(2, e.literals.size)
}

""",
                    ),
            )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        result.invokeMainMethod("mytest.MainKt")
    }

}
