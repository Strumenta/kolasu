package com.strumenta.kolasu.playground

import com.strumenta.kolasu.emf.MetamodelsBuilder
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import com.strumenta.kolasu.validation.IssueType
import org.eclipse.emf.common.util.URI
import org.eclipse.emfcloud.jackson.resource.JsonResource
import org.junit.Test
import java.io.StringWriter
import kotlin.test.assertEquals

class ParserTraceTest {

    val mm = MetamodelsBuilder(JsonResource(URI.createFileURI("mm.json")))

    init {
        mm.addMetamodel(
            "com.strumenta.kolasu.playground",
            "http://mypackage.com", "myp"
        )
        mm.provideClass(ANode::class)
    }

    @Test
    fun serializeIssues() {
        val tt = ParsingResult(
            listOf(Issue(IssueType.TRANSLATION, "some issue", IssueSeverity.WARNING)),
            ANode("a", 1),
            "a:1"
        )
        val writer = StringWriter()
        tt.saveForPlayground(mm.resource!!, writer, "foo.json", "  ")
        val trace = writer.toString()
        assertEquals(
            """{
  "name": "foo.json",
  "code": "a:1",
  "ast": {
    "eClass": "https://strumenta.com/starlasu/v2#//Result",
    "root": {
      "eClass": "mm.json#//ANode",
      "name": "a",
      "value": 1
    },
    "issues": [
      {
        "message": "some issue",
        "severity": "WARNING"
      }
    ]
  }
}""",
            trace
        )
    }
}
