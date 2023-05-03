package com.strumenta.kolasu.symbolresolution

import com.strumenta.kolasu.model.*
import com.strumenta.kolasu.model.commonelements.Expression
import com.strumenta.kolasu.model.commonelements.Statement
import com.strumenta.kolasu.model.lionweb.ReflectionBasedMetamodel
import com.strumenta.kolasu.traversing.findAncestorOfType
import org.junit.Test

data class CompilationUnit(
    var content: MutableList<TypeDecl> = mutableListOf(),
) : ASTNode()

open class TypeDecl(
    override val name: String,
) : ASTNode(), Symbol

data class ClassDecl(
    override val name: String,
    var superclass: ReferenceByName<ClassDecl>? = null,
    var features: MutableList<FeatureDecl> = mutableListOf(),
    var operations: MutableList<OperationDecl> = mutableListOf()
) : TypeDecl(name)

data class FeatureDecl(
    override val name: String,
    var type: ReferenceByName<TypeDecl>,
) : ASTNode(), Symbol

data class OperationDecl(
    override val name: String,
    var parameters: MutableList<ParameterDecl> = mutableListOf(),
    var statements: MutableList<StmtNode> = mutableListOf(),
    var returns: ReferenceByName<TypeDecl>? = null,
) : ASTNode(), Symbol

data class ParameterDecl(
    override val name: String,
    var type: ReferenceByName<TypeDecl>,
) : ASTNode(), Symbol


sealed class StmtNode : ASTNode(), Statement

data class DeclarationStmt(
    override val name: String,
    var value: ExprNode? = null
) : StmtNode(), Symbol

data class AssignmentStmt(
    var lhs: ExprNode,
    var rhs: ExprNode
) : StmtNode()

sealed class ExprNode : ASTNode(), Expression

// a.v.c.d
data class RefExpr(
    var context: ExprNode? = null,
    var symbol: ReferenceByName<Symbol>
) : ExprNode()

data class CallExpr(
    var operation: ReferenceByName<OperationDecl>,
    var arguments: MutableList<ExprNode> = mutableListOf()
) : ExprNode()

data class NewExpr(
    var clazz: ReferenceByName<ClassDecl>
) : ExprNode()

val symbolResolver = declarativeSymbolResolver {

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

object Metamodel : ReflectionBasedMetamodel(CompilationUnit::class, ClassDecl::class)

class SymbolResolutionTest {

    @Test
    fun example() {
        val compilationUnit = CompilationUnit(
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

        symbolResolver.resolveNode(compilationUnit, true)

        compilationUnit.assertAllReferencesResolved()

        println(compilationUnit)
    }
}
