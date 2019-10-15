package com.strumenta.kolasu.generation

import com.strumenta.kolasu.model.Node
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
        assertEquals("""<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<root type="MyRoot">
    <mainSection name="Section1" type="Section"/>
</root>
""", xml)
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
}
