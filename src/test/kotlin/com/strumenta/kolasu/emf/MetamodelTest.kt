package com.strumenta.kolasu.emf

import com.strumenta.kolasu.model.Node
import org.eclipse.emf.ecore.EClass
import org.junit.Test
import kotlin.test.assertEquals

sealed class Statement : Node()
sealed class Expression : Node()
class VarDeclaration(var name: String, var initialValue: Expression) : Statement()
class StringLiteral(var value: String) : Expression()
data class CompilationUnit(val statements: List<Statement>) : Node()

class MetamodelTest {

    @Test
    fun generateSimpleMetamodel() {
        val metamodelBuilder = MetamodelBuilder("SimpleMM")
        metamodelBuilder.addClass(CompilationUnit::class)
        var ePackage = metamodelBuilder.generate()
        assertEquals("SimpleMM", ePackage.name)
        assertEquals(5, ePackage.eClassifiers.size)

        val cu : EClass = ePackage.eClassifiers.find { it.name == "CompilationUnit" } as EClass
        assertEquals(0, cu.eAllAttributes.size)

        val sl : EClass = ePackage.eClassifiers.find { it.name == "StringLiteral" } as EClass
        assertEquals(1, sl.eAllAttributes.size)

        val vd : EClass = ePackage.eClassifiers.find { it.name == "VarDeclaration" } as EClass
        assertEquals(1, vd.eAllAttributes.size)
    }
}