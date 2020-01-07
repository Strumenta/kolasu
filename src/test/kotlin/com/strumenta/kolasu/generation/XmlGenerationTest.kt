package com.strumenta.kolasu.generation

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.Result
import kotlin.test.assertEquals
import org.junit.Test

class XmlGenerationTest {

    @Test
    fun generateXMBasic() {
        val myRoot = MyRoot(
            mainSection = Section(
                "Section1",
                emptyList()
            ),
            otherSections = listOf()
        )
        val xml = XMLGenerator().generateString(myRoot)
        assertEquals(
            """<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<root type="MyRoot">
    <mainSection name="Section1" type="Section"/>
</root>
""",
            xml
        )
    }

    @Test
    fun generateXML() {
        val myRoot = MyRoot(
            mainSection = Section(
                "Section1",
                listOf(
                    Content(1, null),
                    Content(2, Content(3, Content(4, null)))
                )
            ),
            otherSections = listOf()
        )
        val xml = XMLGenerator().generateString(myRoot)
        assertEquals(
            """<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<root type="MyRoot">
    <mainSection name="Section1" type="Section">
        <contents id="1" type="Content">
            <annidatedContent/>
        </contents>
        <contents id="2" type="Content">
            <annidatedContent id="3" type="Content">
                <annidatedContent id="4" type="Content">
                    <annidatedContent/>
                </annidatedContent>
            </annidatedContent>
        </contents>
    </mainSection>
</root>
""",
            xml
        )
    }

    @Test
    fun generateXMLWithListOfValues() {
        val myRoot = MyRoot(
            mainSection = Section(
                "Section1",
                listOf(
                    Content(1, null),
                    OtherContent(listOf(1, 2, 3, 100, -122)),
                    Content(2, Content(3, Content(4, null)))
                )
            ),
            otherSections = listOf()
        )
        val xml = XMLGenerator().generateString(myRoot)
        assertEquals(
            """<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<root type="MyRoot">
    <mainSection name="Section1" type="Section">
        <contents id="1" type="Content">
            <annidatedContent/>
        </contents>
        <contents type="OtherContent">
            <values value="1"/>
            <values value="2"/>
            <values value="3"/>
            <values value="100"/>
            <values value="-122"/>
        </contents>
        <contents id="2" type="Content">
            <annidatedContent id="3" type="Content">
                <annidatedContent id="4" type="Content">
                    <annidatedContent/>
                </annidatedContent>
            </annidatedContent>
        </contents>
    </mainSection>
</root>
""",
            xml
        )
    }

    @Test
    fun generateXMLWithErrors() {
        val issues: List<Issue> = listOf<Issue>(
            Issue.lexical("lexical problem"),
            Issue.semantic("semantic problem", Position(Point(10, 1), Point(12, 3)))
        )
        val result = Result<Node>(issues, null)
        val serialized = XMLGenerator().generateString(result)
        assertEquals(
            """<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<result>
    <errors>
        <Issue message="lexical problem" type="LEXICAL"/>
        <Issue message="semantic problem" type="SEMANTIC">
            <position description="Position(start=Line 10, Column 1, end=Line 12, Column 3)">
                <start column="1" line="10"/>
                <end column="3" line="12"/>
            </position>
        </Issue>
    </errors>
    <root/>
</result>""",
            serialized.trim()
        )
    }
}
