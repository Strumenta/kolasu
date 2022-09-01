package com.strumenta.kolasu.playground

import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import com.strumenta.kolasu.validation.IssueType
import com.strumenta.kolasu.validation.Result
import org.junit.Test
import kotlin.test.assertEquals

data class ANode(override val name: String, val value: Int) : Node(), Named

class TranspilationTraceTest {

    @Test
    fun serializeTranspilationIssues() {
        val tt = TranspilationTrace("a:1", "b:2", ANode("a", 1), ANode("b", 2),
            listOf(Issue(IssueType.TRANSPILATION, "some issue", IssueSeverity.WARNING)))
        assertEquals("", tt.saveAsJson())
    }

    @Test
    fun serializeSourceIssues() {
        val tt = TranspilationTrace("a:1", "b:2",
            Result(listOf(Issue(IssueType.SYNTACTIC, "some issue", IssueSeverity.WARNING)), ANode("a", 1)),
            Result(emptyList(), ANode("b", 2)))
    }

    @Test
    fun serializeTargetIssues() {
        val tt = TranspilationTrace("a:1", "b:2",
            Result(emptyList(), ANode("a", 1)),
            Result(listOf(Issue(IssueType.SYNTACTIC, "some issue", IssueSeverity.WARNING)), ANode("b", 2)))
    }
}