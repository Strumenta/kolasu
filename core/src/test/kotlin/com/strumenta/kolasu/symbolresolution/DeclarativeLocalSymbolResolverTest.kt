package com.strumenta.kolasu.symbolresolution

import com.strumenta.kolasu.model.*
import com.strumenta.kolasu.traversing.findAncestorOfType
import org.junit.Test

data class CompilationUnit(
    var content: MutableList<TypeDecl> = mutableListOf(),
) : Node()

open class TypeDecl(
    override val name: String,
) : Node(), Named

data class ClassDecl(
    override val name: String,
    var superclass: ReferenceByName<ClassDecl>? = null,
    var features: MutableList<FeatureDecl> = mutableListOf(),
    var operations: MutableList<OperationDecl> = mutableListOf(),
) : TypeDecl(name)

data class FeatureDecl(
    override val name: String,
    var type: ReferenceByName<TypeDecl>,
) : Node(), Named

data class OperationDecl(
    override val name: String,
    var parameters: MutableList<ParameterDecl> = mutableListOf(),
    var statements: MutableList<StmtNode> = mutableListOf(),
    var returns: ReferenceByName<TypeDecl>? = null,
) : Node(), Named

data class ParameterDecl(
    override val name: String,
    var type: ReferenceByName<TypeDecl>,
) : Node(), PossiblyNamed

sealed class StmtNode : Node(), Statement

data class DeclarationStmt(
    override val name: String,
    var value: ExprNode? = null,
) : StmtNode(), Named

data class AssignmentStmt(
    var lhs: ExprNode,
    var rhs: ExprNode,
) : StmtNode()

sealed class ExprNode : Node(), Expression

data class RefExpr(
    var context: ExprNode? = null,
    var symbol: ReferenceByName<Named>,
) : ExprNode()

data class CallExpr(
    var operation: ReferenceByName<OperationDecl>,
    var arguments: MutableList<ExprNode> = mutableListOf(),
) : ExprNode()

data class NewExpr(
    var clazz: ReferenceByName<ClassDecl>,
) : ExprNode()

class SymbolResolutionTest {

    @Test
    fun testSymbolResolution() {
        getCompilationUnit()
            // pre-condition
            .apply { assertNotAllReferencesResolved() }
            // resolution
            .apply { getFullSymbolResolver().resolveSymbols(this) }
            // post-condition
            .apply { assertAllReferencesResolved() }
    }

    @Test
    fun testIncrementalSymbolResolutionDevelopment() {
        getCompilationUnit()
            // pre-condition - v1
            .apply { assertNotAllReferencesResolved(ClassDecl::superclass) }
            .apply { assertNotAllReferencesResolved(FeatureDecl::type) }
            .apply { assertNotAllReferencesResolved(RefExpr::symbol) }
            .apply { assertNotAllReferencesResolved(CallExpr::operation) }
            .apply { assertNotAllReferencesResolved(OperationDecl::returns) }
            .apply { assertNotAllReferencesResolved() }
            // resolution - v1
            .apply { getPartialSymbolResolver().resolveSymbols(this) }
            // post-condition - v1 (pre-condition - v2)
            .apply { assertAllReferencesResolved(ClassDecl::superclass) }
            .apply { assertNotAllReferencesResolved(FeatureDecl::type) }
            .apply { assertNotAllReferencesResolved(RefExpr::symbol) }
            .apply { assertNotAllReferencesResolved(CallExpr::operation) }
            .apply { assertNotAllReferencesResolved(OperationDecl::returns) }
            .apply { assertNotAllReferencesResolved() }
            // resolution - v2
            .apply { getFullSymbolResolver().resolveSymbols(this) }
            // post-condition - v2
            .apply { assertAllReferencesResolved(ClassDecl::superclass) }
            .apply { assertAllReferencesResolved(FeatureDecl::type) }
            .apply { assertAllReferencesResolved(RefExpr::symbol) }
            .apply { assertAllReferencesResolved(CallExpr::operation) }
            .apply { assertAllReferencesResolved(OperationDecl::returns) }
            .apply { assertAllReferencesResolved() }
    }

    private fun getCompilationUnit() = CompilationUnit(
        content = mutableListOf(
            ClassDecl(
                name = "class_0",
                features = mutableListOf(
                    FeatureDecl(
                        name = "feature_0",
                        type = ReferenceByName(name = "class_1"),
                    ),
                ),
                operations = mutableListOf(
                    OperationDecl(
                        name = "operation_0",
                        returns = ReferenceByName(name = "class_0"),
                        statements = mutableListOf(
                            AssignmentStmt(
                                lhs = RefExpr(
                                    context = CallExpr(
                                        operation = ReferenceByName(name = "operation_0"),
                                    ),
                                    symbol = ReferenceByName(name = "feature_0"),
                                ),
                                rhs = RefExpr(
                                    context = CallExpr(
                                        operation = ReferenceByName(name = "operation_0"),
                                    ),
                                    symbol = ReferenceByName(name = "feature_0"),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            ClassDecl("class_1"),
        ),
    ).apply { assignParents() }

    private fun getFullSymbolResolver() = symbolResolver {
        scopeFor(ClassDecl::superclass) { compilationUnit: CompilationUnit ->
            Scope().apply { compilationUnit.content.filterIsInstance<ClassDecl>().forEach { define(it) } }
        }

        scopeFor(FeatureDecl::type) { compilationUnit: CompilationUnit ->
            Scope().apply { compilationUnit.content.forEach { define(it) } }
        }

        scopeFor(RefExpr::symbol) { refExpr: RefExpr ->
            var scope: Scope? = null
            if (refExpr.context != null) {
                scope = getScope(RefExpr::symbol, refExpr.context!!)
            }
            scope
        }

        scopeFor(RefExpr::symbol) { callExpr: CallExpr ->
            val scope = Scope()
            if (!callExpr.operation.resolved) {
                resolveProperty(CallExpr::operation, callExpr)
            }
            if (callExpr.operation.referred != null && !callExpr.operation.referred!!.returns!!.resolved) {
                resolveProperty(OperationDecl::returns, callExpr.operation.referred!!)
            }
            if (callExpr.operation.referred!!.returns!!.referred != null) {
                val returnType = callExpr.operation.referred!!.returns!!.referred!!
                if (returnType is ClassDecl) {
                    returnType.features.forEach { scope.define(it) }
                    returnType.operations.forEach { scope.define(it) }
                }
            }
            scope
        }

        scopeFor(RefExpr::symbol) { newExpr: NewExpr ->
            val scope = Scope()
            if (!newExpr.clazz.resolved) {
                resolveProperty(NewExpr::clazz, newExpr)
            }
            if (newExpr.clazz.referred != null) {
                val returnType = newExpr.clazz.referred
                if (returnType is ClassDecl) {
                    returnType.features.forEach { scope.define(it) }
                    returnType.operations.forEach { scope.define(it) }
                }
            }
            scope
        }

        scopeFor(CallExpr::operation) { callExpr: CallExpr ->
            callExpr.findAncestorOfType(ClassDecl::class.java)?.let {
                getScope(CallExpr::operation, it)
            }
        }

        scopeFor(CallExpr::operation) { classDecl: ClassDecl ->
            Scope().apply { classDecl.operations.forEach { define(it) } }
        }

        scopeFor(OperationDecl::returns) { operationDecl: OperationDecl ->
            operationDecl.findAncestorOfType(CompilationUnit::class.java)?.let {
                getScope(OperationDecl::returns, it)
            }
        }

        scopeFor(OperationDecl::returns) { compilationUnit: CompilationUnit ->
            Scope().apply { compilationUnit.content.forEach { this.define(it) } }
        }
    }

    private fun getPartialSymbolResolver() = symbolResolver {
        scopeFor(ClassDecl::superclass) { compilationUnit: CompilationUnit ->
            Scope().apply { compilationUnit.content.filterIsInstance<ClassDecl>().forEach { define(it) } }
        }
    }
}
