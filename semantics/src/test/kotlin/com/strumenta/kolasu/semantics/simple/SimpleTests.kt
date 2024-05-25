package com.strumenta.kolasu.semantics.simple

import com.strumenta.kolasu.model.*
import com.strumenta.kolasu.semantics.scoping.scope
import com.strumenta.kolasu.semantics.services.SymbolResolver
import com.strumenta.kolasu.testing.assertReferencesNotResolved
import com.strumenta.kolasu.testing.assertReferencesResolved
import com.strumenta.kolasu.traversing.walk
import org.junit.Test
import kotlin.test.assertEquals

class SimpleTests {

    @Test
    fun testSymbolResolution() {
        val symbolResolver = SymbolResolver(simpleReferenceResolver)
        getCompilationUnit()
            .apply { assertReferencesNotResolved() }
            .apply { symbolResolver.resolveTree(this) }
            .apply { assertReferencesResolved() }
    }

    @Test
    fun testTypeComputation() {
        val compilationUnit = getCompilationUnit()
        val featureDeclaration = compilationUnit.walk()
            .filterIsInstance<FeatureDecl>()
            .find { featureDeclaration -> featureDeclaration.name == "feature_0" }!!
        assertEquals(
            compilationUnit.walk()
                .filterIsInstance<ClassDecl>()
                .find { classDeclaration -> classDeclaration.name == "class_1" },
            simpleTypeComputer.typeFor(featureDeclaration)
        )
    }

    @Test
    fun testScopeIgnoringCase() {
        val expectedClassDecl = ClassDecl("TestNode")
        val scope = scope { define(expectedClassDecl.name, expectedClassDecl) }
        val foundClassDecl = scope.entries<ClassDecl> { (name) -> name.lowercase() == "testnode" }.firstOrNull()
        assertEquals(expectedClassDecl, foundClassDecl)
    }

    @Test
    fun testScopeConsideringCase() {
        val expectedClassDecl = ClassDecl(name = "TestNode")
        val scope = scope { define(expectedClassDecl.name, expectedClassDecl) }
        val foundClassDecl = scope.entries<ClassDecl> { (name) -> name == "TestNode" }.firstOrNull()
        assertEquals(expectedClassDecl, foundClassDecl)
    }

    private fun getCompilationUnit() = CompilationUnit(
        content = mutableListOf(
            ClassDecl(
                name = "class_0",
                features = mutableListOf(
                    FeatureDecl(
                        name = "feature_0",
                        type = ReferenceByName(name = "class_1")
                    )
                ),
                operations = mutableListOf(
                    OperationDecl(
                        name = "operation_0",
                        returns = ReferenceByName(name = "class_0"),
                        statements = mutableListOf(
                            AssignmentStmt(
                                lhs = RefExpr(
                                    context = CallExpr(
                                        operation = ReferenceByName(name = "operation_0")
                                    ),
                                    symbol = ReferenceByName(name = "feature_0")
                                ),
                                rhs = RefExpr(
                                    context = CallExpr(
                                        operation = ReferenceByName(name = "operation_0")
                                    ),
                                    symbol = ReferenceByName(name = "feature_0")
                                )
                            )
                        )
                    )
                )
            ),
            ClassDecl("class_1")
        )
    ).apply { assignParents() }
}
