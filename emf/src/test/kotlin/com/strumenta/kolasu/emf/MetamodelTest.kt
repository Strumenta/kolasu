package com.strumenta.kolasu.emf

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Position
import org.eclipse.emf.ecore.EClass
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import kotlin.test.assertEquals

sealed class Statement : Node()
sealed class Expression : Node()
enum class Visibility {
    PUBLIC, PRIVATE
}
class VarDeclaration(var visibility: Visibility, var name: String, var initialValue: Expression) : Statement()
class StringLiteral(var value: String) : Expression()
class LocalDateTimeLiteral(var value: LocalDateTime) : Expression()
data class CompilationUnit(val statements: List<Statement>?, override var specifiedPosition: Position? = null) :
    Node(specifiedPosition)

class MetamodelTest {

    private fun temporaryFile(suffix: String): File {
        val f = kotlin.io.path.createTempFile(suffix = suffix).toFile()
        f.deleteOnExit()
        return f
    }

    @Test
    fun generateSimpleMetamodel() {
        val metamodelBuilder = MetamodelBuilder("SimpleMM", "https://strumenta.com/simplemm", "simplemm")
        metamodelBuilder.provideClass(CompilationUnit::class)
        val ePackage = metamodelBuilder.generate()
        ePackage.saveEcore(temporaryFile("simplemm.ecore"))
        ePackage.saveAsJson(temporaryFile("simplemm.json"))
        assertEquals("SimpleMM", ePackage.name)
        assertEquals(7, ePackage.eClassifiers.size)

        val cu: EClass = ePackage.eClassifiers.find { it.name == "CompilationUnit" } as EClass
        assertEquals(1, cu.eAllSuperTypes.size)
        assertEquals(0, cu.eAllAttributes.size)
        assertEquals(2, cu.eAllContainments.size)
        assertEquals(2, cu.eAllReferences.size, cu.eAllReferences.joinToString(", ") { it.name })
        assertEquals(2, cu.eAllStructuralFeatures.size)

        val e: EClass = ePackage.eClassifiers.find { it.name == "Expression" } as EClass
        assertEquals(true, e.isAbstract)

        val sl: EClass = ePackage.eClassifiers.find { it.name == "StringLiteral" } as EClass
        assertEquals(
            2, sl.eAllSuperTypes.size,
            sl.eAllSuperTypes.joinToString(", ") { it.name }
        )
        assertEquals(1, sl.eAllAttributes.size)
        assertEquals(1, sl.eAllContainments.size, sl.eAllContainments.joinToString(", ") { it.name })
        assertEquals(1, sl.eAllReferences.size, sl.eAllReferences.joinToString(", ") { it.name })
        assertEquals(2, sl.eAllStructuralFeatures.size)

        val vd: EClass = ePackage.eClassifiers.find { it.name == "VarDeclaration" } as EClass
        assertEquals(2, vd.eAllAttributes.size)
    }
}
