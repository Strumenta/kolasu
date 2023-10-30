import com.strumenta.kolasu.lionweb.ASTGenerator
import com.strumenta.kolasu.lionweb.LWLanguage
import com.strumenta.kolasu.lionweb.StarLasuLWLanguage
import io.lionweb.lioncore.java.language.Interface
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.language.LionCoreBuiltins
import io.lionweb.lioncore.java.language.Property
import io.lionweb.lioncore.java.serialization.JsonSerialization
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ASTGeneratorTest {

    @Test
    fun allASTClassesAreGeneratedAsExpected() {
        val inputStream = this.javaClass.getResourceAsStream("/properties-language.json")
        val jsonser = JsonSerialization.getStandardSerialization()
        jsonser.instanceResolver.addTree(StarLasuLWLanguage)
        val propertiesLanguage = jsonser.deserializeToNodes(inputStream).first() as Language
        val generated = ASTGenerator("com.strumenta.properties", propertiesLanguage).generateClasses()
        assertEquals(1, generated.size)
        assertEquals(
            """package com.strumenta.properties

import com.strumenta.kolasu.lionweb.LionWebAssociation
import com.strumenta.kolasu.model.Node
import kotlin.Boolean
import kotlin.String
import kotlin.collections.MutableList

@LionWebAssociation(key = "io-lionweb-Properties_PropertiesFile")
public data class PropertiesFile(
  public var props: MutableList<Property>,
) : Node()

@LionWebAssociation(key = "io-lionweb-Properties_Property")
public data class Property(
  public var name: String,
  public var `value`: Value,
) : Node()

@LionWebAssociation(key = "io-lionweb-Properties_Value")
public sealed class Value : Node()

@LionWebAssociation(key = "io-lionweb-Properties_BooleanValue")
public data class BooleanValue(
  public var `value`: Boolean,
) : Value()

@LionWebAssociation(key = "io-lionweb-Properties_DecValue")
public data class DecValue(
  public var `value`: String,
) : Value()

@LionWebAssociation(key = "io-lionweb-Properties_IntValue")
public data class IntValue(
  public var `value`: String,
) : Value()

@LionWebAssociation(key = "io-lionweb-Properties_StringValue")
public data class StringValue(
  public var `value`: String,
) : Value()""".trim(),
            generated.first().code.trim()
        )
    }

    @Test
    fun generateInterface() {
        val dummyLanguage = LWLanguage()
        dummyLanguage.addElement(
            Interface().apply {
                name = "MyInterface"
                key = "MyKey"
                addFeature(Property.createRequired("someFlag", LionCoreBuiltins.getBoolean()))
            }
        )
        val generated = ASTGenerator("com.strumenta.example", dummyLanguage).generateClasses()
        assertEquals(1, generated.size)
        assertEquals(
            """package com.strumenta.example

import com.strumenta.kolasu.lionweb.LionWebAssociation
import com.strumenta.kolasu.model.NodeType
import kotlin.Boolean

@LionWebAssociation(key = "MyKey")
@NodeType
public interface MyInterface {
  public var someFlag: Boolean = someFlag
}""".trim(),
            generated.first().code.trim()
        )
    }
}
