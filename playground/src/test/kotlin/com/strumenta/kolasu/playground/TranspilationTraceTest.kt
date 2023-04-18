package com.strumenta.kolasu.playground

import com.strumenta.kolasu.emf.MetamodelBuilder
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import com.strumenta.kolasu.validation.IssueType
import com.strumenta.kolasu.validation.Result
import org.junit.Test
import kotlin.test.assertEquals

class TranspilationTraceTest {

    val mm = MetamodelBuilder(
        "com.strumenta.kolasu.playground",
        "http://mypackage.com", "myp"
    ).apply { provideClass(ANode::class) }.generate()

    @Test
    fun serializeTranslationIssues() {
        val tt = TranspilationTrace(
            "a:1", "b:2", ANode("a", 1), ANode("b", 2),
            listOf(Issue(IssueType.TRANSLATION, "some issue", IssueSeverity.WARNING))
        )
        assertEquals(
            """{
  "eClass" : "https://strumenta.com/starlasu/transpilation/v1#//TranspilationTrace",
  "originalCode" : "a:1",
  "sourceResult" : {
    "root" : {
      "eClass" : "http://mypackage.com#//ANode",
      "name" : "a",
      "value" : 1
    }
  },
  "targetResult" : {
    "root" : {
      "eClass" : "http://mypackage.com#//ANode",
      "name" : "b",
      "value" : 2
    }
  },
  "generatedCode" : "b:2",
  "issues" : [ {
    "message" : "some issue",
    "severity" : "WARNING"
  } ]
}""",
            tt.saveAsJson("foo.json", mm)
        )
    }

    @Test
    fun serializeSourceIssues() {
        val tt = TranspilationTrace(
            "a:1", "b:2",
            Result(listOf(Issue(IssueType.SYNTACTIC, "some issue", IssueSeverity.WARNING)), ANode("a", 1)),
            Result(emptyList(), ANode("b", 2))
        )
        assertEquals(
            """{
  "eClass" : "https://strumenta.com/starlasu/transpilation/v1#//TranspilationTrace",
  "originalCode" : "a:1",
  "sourceResult" : {
    "root" : {
      "eClass" : "http://mypackage.com#//ANode",
      "name" : "a",
      "value" : 1
    },
    "issues" : [ {
      "type" : "SYNTACTIC",
      "message" : "some issue",
      "severity" : "WARNING"
    } ]
  },
  "targetResult" : {
    "root" : {
      "eClass" : "http://mypackage.com#//ANode",
      "name" : "b",
      "value" : 2
    }
  },
  "generatedCode" : "b:2"
}""",
            tt.saveAsJson("foo.json", mm)
        )
    }

    @Test
    fun serializeTargetIssues() {
        val tt = TranspilationTrace(
            "a:1", "b:2",
            Result(emptyList(), ANode("a", 1)),
            Result(listOf(Issue(IssueType.SYNTACTIC, "some issue", IssueSeverity.WARNING)), ANode("b", 2))
        )
        assertEquals(
            """{
  "eClass" : "https://strumenta.com/starlasu/transpilation/v1#//TranspilationTrace",
  "originalCode" : "a:1",
  "sourceResult" : {
    "root" : {
      "eClass" : "http://mypackage.com#//ANode",
      "name" : "a",
      "value" : 1
    }
  },
  "targetResult" : {
    "root" : {
      "eClass" : "http://mypackage.com#//ANode",
      "name" : "b",
      "value" : 2
    },
    "issues" : [ {
      "type" : "SYNTACTIC",
      "message" : "some issue",
      "severity" : "WARNING"
    } ]
  },
  "generatedCode" : "b:2"
}""",
            tt.saveAsJson("foo.json", mm)
        )
    }

    @Test
    fun serializeSourceAndDestination() {
        val aRoot = ANode("a", 1)
        val bRoot = ANode("b", 2)
        aRoot.destination = bRoot
        bRoot.origin = aRoot
        val tt = TranspilationTrace(
            "a:1", "b:2",
            Result(emptyList(), aRoot),
            Result(emptyList(), bRoot)
        )
        assertEquals(
            """{
  "eClass" : "https://strumenta.com/starlasu/transpilation/v1#//TranspilationTrace",
  "originalCode" : "a:1",
  "sourceResult" : {
    "root" : {
      "eClass" : "http://mypackage.com#//ANode",
      "destination" : {
        "eClass" : "https://strumenta.com/starlasu/v2#//NodeDestination",
        "node" : {
          "eClass" : "http://mypackage.com#//ANode",
          "${'$'}ref" : "//@targetResult/@root"
        }
      },
      "name" : "a",
      "value" : 1
    }
  },
  "targetResult" : {
    "root" : {
      "eClass" : "http://mypackage.com#//ANode",
      "origin" : {
        "eClass" : "https://strumenta.com/starlasu/v2#//NodeOrigin",
        "node" : {
          "eClass" : "http://mypackage.com#//ANode",
          "${'$'}ref" : "//@sourceResult/@root"
        }
      },
      "name" : "b",
      "value" : 2
    }
  },
  "generatedCode" : "b:2"
}""",
            tt.saveAsJson("foo.json", mm)
        )
    }
}
