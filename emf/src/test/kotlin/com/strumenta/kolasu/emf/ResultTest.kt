package com.strumenta.kolasu.emf

import com.strumenta.kolasu.emf.serialization.JsonGenerator
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueType
import com.strumenta.kolasu.validation.Result
import org.junit.Test

class ResultTest {

    @Test
    fun generateSimpleModelResult() {
        val cu = CompilationUnit(
            listOf(
                VarDeclaration(Visibility.PUBLIC, "a", StringLiteral("foo")),
                VarDeclaration(Visibility.PRIVATE, "b", StringLiteral("bar"))
            )
        )
        val metamodelBuilder = MetamodelBuilder("SimpleMM", "https://strumenta.com/simplemm", "simplemm")
        metamodelBuilder.provideClass(CompilationUnit::class)
        var ePackage = metamodelBuilder.generate()

        val eo = cu.toEObject(ePackage)

        val result = Result(
            listOf(
                Issue(IssueType.LEXICAL, "lex", Position(Point(1, 1), Point(2, 10))),
                Issue(IssueType.SEMANTIC, "foo")
            ),
            cu
        )

        // eo.saveXMI(File("simplemodel.xmi"))
        println(JsonGenerator().generateEMFString(result, ePackage))
    }
}
