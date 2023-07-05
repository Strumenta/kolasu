import com.strumenta.kolasu.lionweb.KotlinCodeProcessor
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class RecognizeExistingASTClassesTest {

    @Test
    fun simpleCase() {
        val code = """package com.strumenta.props

import com.strumenta.kolasu.model.Node
import kotlin.Boolean
import kotlin.String
import kotlin.collections.MutableList

public data class PropertiesFile(
  public var props: MutableList<Property>,
) : Node()

public data class Property(
  public var name: String,
  public var `value`: Value,
) : Node()"""
        val existingClasses = KotlinCodeProcessor().classesDeclaredInFile(code)
        assertEquals(setOf("com.strumenta.props.PropertiesFile", "com.strumenta.props.Property"), existingClasses)

        val existingASTClasses = KotlinCodeProcessor().classesDeclaredInFile(code)
        assertEquals(setOf("com.strumenta.props.PropertiesFile", "com.strumenta.props.Property"), existingASTClasses)
    }

    @Test
    @Ignore
    fun caseWithNodesAndOtherClasses() {
        val code = """package com.strumenta.props

import com.strumenta.kolasu.model.Node
import kotlin.Boolean
import kotlin.String
import kotlin.collections.MutableList

public data class PropertiesFile(
  public var props: MutableList<Property>,
) : Node()

public data class SomeOtherClass(val n: String)

public data class Property(
  public var name: String,
  public var `value`: Value,
) : Node()"""
        val existingClasses = KotlinCodeProcessor().classesDeclaredInFile(code)
        assertEquals(
            setOf(
                "com.strumenta.props.PropertiesFile", "com.strumenta.props.SomeOtherClass",
                "com.strumenta.props.Property"
            ),
            existingClasses
        )

        val existingASTClasses = KotlinCodeProcessor().classesDeclaredInFile(code)
        assertEquals(setOf("com.strumenta.props.PropertiesFile", "com.strumenta.props.Property"), existingASTClasses)
    }
}
