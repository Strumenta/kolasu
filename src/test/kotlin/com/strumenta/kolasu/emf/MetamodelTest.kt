package com.strumenta.kolasu.emf

import com.strumenta.kolasu.model.Node
import java.io.File
import kotlin.test.assertEquals
import org.eclipse.emf.ecore.EClass
import org.junit.Test

sealed class Statement : Node()
sealed class Expression : Node()
enum class Visibility {
    PUBLIC, PRIVATE
}
class VarDeclaration(var visibility: Visibility, var name: String, var initialValue: Expression) : Statement()
class StringLiteral(var value: String) : Expression()
data class CompilationUnit(val statements: List<Statement>) : Node()

class MetamodelTest {

    @Test
    fun generateSimpleMetamodel() {
        val metamodelBuilder = MetamodelBuilder("SimpleMM", "https://strumenta.com/simplemm", "simplemm")
        metamodelBuilder.addClass(CompilationUnit::class)
        val ePackage = metamodelBuilder.generate()
        ePackage.saveEcore(File("simplemm.ecore"))
        ePackage.saveAsJson(File("simplemm.json"))
        assertEquals("SimpleMM", ePackage.name)
        assertEquals(7, ePackage.eClassifiers.size)

        val cu: EClass = ePackage.eClassifiers.find { it.name == "CompilationUnit" } as EClass
        assertEquals(0, cu.eAllSuperTypes.size)
        assertEquals(0, cu.eAllAttributes.size)
        assertEquals(1, cu.eAllContainments.size)
        assertEquals(1, cu.eAllReferences.size)
        assertEquals(1, cu.eAllStructuralFeatures.size)

        val e: EClass = ePackage.eClassifiers.find { it.name == "Expression" } as EClass
        assertEquals(true, e.isAbstract)

        val sl: EClass = ePackage.eClassifiers.find { it.name == "StringLiteral" } as EClass
        assertEquals(1, sl.eAllSuperTypes.size)
        assertEquals(1, sl.eAllAttributes.size)
        assertEquals(0, sl.eAllContainments.size)
        assertEquals(0, sl.eAllReferences.size)
        assertEquals(1, sl.eAllStructuralFeatures.size)

        val vd: EClass = ePackage.eClassifiers.find { it.name == "VarDeclaration" } as EClass
        assertEquals(2, vd.eAllAttributes.size)
    }
}
