package com.strumenta.kolasu.model

import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.language.explore
import java.util.LinkedList
import kotlin.test.assertEquals
import kotlin.test.assertSame
import org.junit.Test as test

object MyLanguage3 : StarLasuLanguage("com.foo.a3") {
    init {
        explore(A::class, AW::class, BW::class, DW::class, B::class, CW::class)
    }
}

@LanguageAssociation(MyLanguage3::class)
data class A(
    val s: String,
) : Node()

@LanguageAssociation(MyLanguage3::class)
data class B(
    val a: A,
    val manyAs: List<A>,
) : Node()

@LanguageAssociation(MyLanguage3::class)
data class AW(
    var s: String,
) : Node()

@LanguageAssociation(MyLanguage3::class)
data class BW(
    var a: AW,
    val manyAs: MutableList<AW>,
) : Node()

@LanguageAssociation(MyLanguage3::class)
data class CW(
    var a: AW,
    val manyAs: MutableList<AW>,
) : Node()

@LanguageAssociation(MyLanguage3::class)
data class DW(
    var a: BW,
    val manyAs: MutableList<AW>,
) : Node()

@LanguageAssociation(MyLanguage3::class)
interface FooNodeType : NodeLike

interface BarNotNodeType

@LanguageAssociation(MyLanguage3::class)
data class MiniCalcFile(
    val elements: List<MCStatement>,
) : Node()

@LanguageAssociation(MyLanguage3::class)
data class VarDeclaration(
    override val name: String,
    val value: MCExpression,
) : MCStatement(),
    Named

@LanguageAssociation(MyLanguage3::class)
sealed class MCExpression : Node()

@LanguageAssociation(MyLanguage3::class)
data class IntLit(
    val value: String,
) : MCExpression()

@LanguageAssociation(MyLanguage3::class)
sealed class MCStatement : Node()

@LanguageAssociation(MyLanguage3::class)
data class Assignment(
    val ref: ReferenceValue<VarDeclaration>,
    val value: MCExpression,
) : MCStatement()

@LanguageAssociation(MyLanguage3::class)
data class Print(
    val value: MCExpression,
) : MCStatement()

@LanguageAssociation(MyLanguage3::class)
data class ValueReference(
    val ref: ReferenceValue<VarDeclaration>,
) : MCExpression()

class ProcessingTest {
    @test
    fun recognizeNodeType() {
        assertEquals(true, FooNodeType::class.isConceptInterface)
        assertEquals(false, BarNotNodeType::class.isConceptInterface)

        assertEquals(true, FooNodeType::class.isANode())
        assertEquals(false, BarNotNodeType::class.isANode())
    }

    @test(expected = ImmutablePropertyException::class)
    fun replaceSingleOnReadOnly() {
        val a1 = A("1")
        val a2 = A("2")
        val b = B(a1, emptyList())
        b.assignParents()
        a1.replaceWith(a2)
    }

    @test
    fun replaceSingle() {
        val a1 = AW("1")
        val a2 = AW("2")
        val b = BW(a1, LinkedList())
        b.assignParents()
        a1.replaceWith(a2)
        assertEquals("2", b.a.s)
    }

    @test
    fun replaceInList() {
        val a1 = AW("1")
        val a2 = AW("2")
        val a3 = AW("3")
        val a4 = AW("4")
        val b = BW(a1, mutableListOf(a2, a3))
        b.assignParents()
        a2.replaceWith(a4)
        assertEquals("4", b.manyAs[0].s)
        assertEquals(BW(a1, mutableListOf(a4, a3)), b)
    }

    @test
    fun replaceSeveralInList() {
        val a1 = AW("1")
        val a2 = AW("2")
        val a3 = AW("3")
        val a4 = AW("4")
        val a5 = AW("5")
        val b = BW(a1, mutableListOf(a2, a3))
        b.replaceWithSeveral(a2, listOf(a4, a5))
        assertEquals("4", b.manyAs[0].s)
        assertEquals("5", b.manyAs[1].s)
        assertEquals("3", b.manyAs[2].s)
    }

    @test
    fun replaceSeveralInListInParent() {
        val a1 = AW("1")
        val a2 = AW("2")
        val a3 = AW("3")
        val a4 = AW("4")
        val a5 = AW("5")
        val b = BW(a1, mutableListOf(a2, a3))
        b.assignParents()
        a2.replaceWithSeveral(listOf(a4, a5))
        assertEquals("4", b.manyAs[0].s)
        assertEquals("5", b.manyAs[1].s)
        assertEquals("3", b.manyAs[2].s)
    }

    @test(expected = IllegalStateException::class)
    fun replaceSeveralInListInParentButTheNodeToReplaceIsMissing() {
        val a1 = AW("1")
        val a2 = AW("2")
        val a3 = AW("3")
        val a4 = AW("4")
        val a5 = AW("5")
        val b = BW(a1, mutableListOf(a2, a3))
        b.assignParents()
        a1.replaceWithSeveral(listOf(a4, a5))
    }

    @test
    fun addSeveralBeforeInListInParent() {
        val notInList = AW("x")
        val existing1 = AW("e1")
        val existing2 = AW("e2")
        val before1 = AW("b1")
        val before2 = AW("b2")
        val parentNode = BW(notInList, mutableListOf(existing1, existing2))
        parentNode.assignParents()
        existing1.addSeveralBefore(listOf(before1))
        existing2.addSeveralBefore(listOf(before2))

        assertSame(before1, parentNode.manyAs[0])
        assertSame(existing1, parentNode.manyAs[1])
        assertSame(before2, parentNode.manyAs[2])
        assertSame(existing2, parentNode.manyAs[3])
    }

    @test
    fun addSeveralAfterInListInParent() {
        val notInList = AW("x")
        val existing1 = AW("e1")
        val existing2 = AW("e2")
        val after1 = AW("a1")
        val after2 = AW("a2")
        val parentNode = BW(notInList, mutableListOf(existing1, existing2))
        parentNode.assignParents()
        existing1.addSeveralAfter(listOf(after1))
        existing2.addSeveralAfter(listOf(after2))

        assertSame(existing1, parentNode.manyAs[0])
        assertSame(after1, parentNode.manyAs[1])
        assertSame(existing2, parentNode.manyAs[2])
        assertSame(after2, parentNode.manyAs[3])
    }

    @test
    fun removeInListInParent() {
        val notInList = AW("x")
        val existing1 = AW("e1")
        val existing2 = AW("e2")
        val parentNode = BW(notInList, mutableListOf(existing1, existing2))
        parentNode.assignParents()
        existing2.removeFromList()

        assertSame(existing1, parentNode.manyAs[0])
        assertEquals(1, parentNode.manyAs.size)
    }

    @test
    fun transformVarName() {
        val startTree =
            MiniCalcFile(
                listOf(
                    VarDeclaration("A", IntLit("10")),
                    Assignment(ReferenceValue("A"), IntLit("11")),
                    Print(ValueReference(ReferenceValue("A"))),
                ),
            )

        val expectedTransformedTree =
            MiniCalcFile(
                listOf(
                    VarDeclaration("B", IntLit("10")),
                    Assignment(ReferenceValue("B"), IntLit("11")),
                    Print(ValueReference(ReferenceValue("B"))),
                ),
            )

        val nodesProcessed = HashSet<NodeLike>()

        assertEquals(
            expectedTransformedTree,
            startTree.transformTree(operation = {
                if (nodesProcessed.contains(it)) {
                    throw RuntimeException("Trying to process again node $it")
                }
                nodesProcessed.add(it)
                when (it) {
                    is VarDeclaration -> VarDeclaration("B", it.value)
                    is ValueReference -> ValueReference(ReferenceValue("B"))
                    is Assignment -> Assignment(ReferenceValue("B"), it.value)
                    else -> it
                }
            }),
        )
    }

    @test
    fun getNextAndPreviousSibling() {
        val a1 = AW("1")
        val a2 = AW("2")
        val a3 = AW("3")
        val a4 = AW("4")
        val b = BW(a1, mutableListOf(a2, a3, a4))
        b.assignParents()
        assertSame(a2, b.manyAs[1].previousSibling)
        assertSame(a4, b.manyAs[1].nextSibling)
    }

    @test
    fun getNextAndPreviousSiblingOfSpecifiedType() {
        val a1 = AW("1")
        val a2 = AW("2")
        val a3 = AW("3")
        val a4 = AW("4")
        val b = BW(a1, mutableListOf(a2, a3, a4))
        val d = DW(b, mutableListOf(a2, a3, a4))
        b.assignParents()

        assert(b.manyAs[1].previousSibling<BW>() == null)
        assert(b.manyAs[1].nextSibling<BW>() == null)
        d.assignParents()
        assertSame(b, d.manyAs[0].previousSibling<BW>())
    }

    @test
    fun getNextAndPreviousSamePropertySibling() {
        val a1 = AW("1")
        val a2 = AW("2")
        val a3 = AW("3")
        val a4 = AW("4")
        val b = BW(a1, mutableListOf(a2, a3, a4))
        b.assignParents()

        assert(b.a.previousSamePropertySibling == null)
        assert(b.a.nextSamePropertySibling == null)
        assertSame(b.manyAs[0], b.manyAs[1].previousSamePropertySibling)
        assertSame(b.manyAs[2], b.manyAs[1].nextSamePropertySibling)
        assertSame(null, b.manyAs[2].nextSamePropertySibling)
    }

    @test
    fun containingProperty() {
        val a1 = AW("1")
        val a2 = AW("2")
        val a3 = AW("3")
        val a4 = AW("4")
        val b = BW(a1, mutableListOf(a2, a3, a4))
        b.assignParents()
        assertEquals(null, b.containingContainment())
        assertEquals("a", a1.containingContainment()?.name)
        assertEquals("manyAs", a2.containingContainment()?.name)
        assertEquals("manyAs", a3.containingContainment()?.name)
        assertEquals("manyAs", a4.containingContainment()?.name)
    }

    @test
    fun containingPropertyWhenNodesAreEquals() {
        val a1 = AW("1")
        val a2 = AW("1")
        val a3 = AW("1")
        val a4 = AW("4")
        val b = BW(a1, mutableListOf(a2, a3, a4))
        b.assignParents()
        assertEquals(null, b.containingContainment())
        assertEquals("a", a1.containingContainment()?.name)
        assertEquals("manyAs", a2.containingContainment()?.name)
        assertEquals("manyAs", a3.containingContainment()?.name)
        assertEquals("manyAs", a4.containingContainment()?.name)
    }

    @test
    fun indexInContainingProperty() {
        val a1 = AW("1")
        val a2 = AW("2")
        val a3 = AW("3")
        val a4 = AW("4")
        val b = BW(a1, mutableListOf(a2, a3, a4))
        b.assignParents()
        assertEquals(null, b.indexInContainingProperty())
        assertEquals(0, a1.indexInContainingProperty())
        assertEquals(0, a2.indexInContainingProperty())
        assertEquals(1, a3.indexInContainingProperty())
        assertEquals(2, a4.indexInContainingProperty())
    }

    @test
    fun indexInContainingPropertyWhenNodeAreEquals() {
        val a1 = AW("1")
        val a2 = AW("2")
        val a3 = AW("3")
        val a4 = AW("2")
        val b = BW(a1, mutableListOf(a2, a3, a4))
        b.assignParents()
        assertEquals(null, b.indexInContainingProperty())
        assertEquals(0, a1.indexInContainingProperty())
        assertEquals(0, a2.indexInContainingProperty())
        assertEquals(1, a3.indexInContainingProperty())
        assertEquals(2, a4.indexInContainingProperty())
    }
}
