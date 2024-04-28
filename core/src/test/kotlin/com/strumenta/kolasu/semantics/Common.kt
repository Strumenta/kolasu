package com.strumenta.kolasu.semantics

import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.language.explore
import com.strumenta.kolasu.model.Expression
import com.strumenta.kolasu.model.LanguageAssociation
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceValue
import com.strumenta.kolasu.model.Statement

object MyExampleLanguageForSemantics : StarLasuLanguage("com.foo.MyExampleLanguageForSemantics") {
    init {
        explore(
            CompilationUnit::class,
            ClassDecl::class,
        )
    }
}

@LanguageAssociation(MyExampleLanguageForSemantics::class)
internal data class CompilationUnit(
    var content: MutableList<TypeDecl> = mutableListOf(),
) : Node()

@LanguageAssociation(MyExampleLanguageForSemantics::class)
internal sealed class SymbolNode(
    override val name: String,
) : Node(),
    Named

@LanguageAssociation(MyExampleLanguageForSemantics::class)
internal open class TypeDecl(
    override val name: String,
) : SymbolNode(name)

@LanguageAssociation(MyExampleLanguageForSemantics::class)
internal data class ClassDecl(
    override val name: String,
    var superclass: ReferenceValue<ClassDecl>? = null,
    var features2: MutableList<FeatureDecl> = mutableListOf(),
    var operations: MutableList<OperationDecl> = mutableListOf(),
) : TypeDecl(name)

@LanguageAssociation(MyExampleLanguageForSemantics::class)
internal data class FeatureDecl(
    override val name: String,
    var type: ReferenceValue<TypeDecl>,
) : SymbolNode(name)

@LanguageAssociation(MyExampleLanguageForSemantics::class)
internal data class OperationDecl(
    override val name: String,
    var parameters: MutableList<ParameterDecl> = mutableListOf(),
    var statements: MutableList<StmtNode> = mutableListOf(),
    var returns: ReferenceValue<TypeDecl>? = null,
) : SymbolNode(name)

@LanguageAssociation(MyExampleLanguageForSemantics::class)
internal data class ParameterDecl(
    override val name: String,
    var type: ReferenceValue<TypeDecl>,
) : SymbolNode(name)

@LanguageAssociation(MyExampleLanguageForSemantics::class)
internal sealed class StmtNode :
    Node(),
    Statement

@LanguageAssociation(MyExampleLanguageForSemantics::class)
internal data class DeclarationStmt(
    val variable: Variable,
    var value: ExprNode? = null,
) : StmtNode()

@LanguageAssociation(MyExampleLanguageForSemantics::class)
internal data class Variable(
    override val name: String,
    var type: ReferenceValue<TypeDecl>,
) : SymbolNode(name)

@LanguageAssociation(MyExampleLanguageForSemantics::class)
internal data class AssignmentStmt(
    var lhs: ExprNode,
    var rhs: ExprNode,
) : StmtNode()

@LanguageAssociation(MyExampleLanguageForSemantics::class)
internal sealed class ExprNode :
    Node(),
    Expression

@LanguageAssociation(MyExampleLanguageForSemantics::class)
internal data class RefExpr(
    var context: ExprNode? = null,
    var symbol: ReferenceValue<SymbolNode>,
) : ExprNode()

@LanguageAssociation(MyExampleLanguageForSemantics::class)
internal data class CallExpr(
    var operation: ReferenceValue<OperationDecl>,
    var arguments: MutableList<ExprNode> = mutableListOf(),
) : ExprNode()

@LanguageAssociation(MyExampleLanguageForSemantics::class)
internal data class NewExpr(
    var clazz: ReferenceValue<ClassDecl>,
) : ExprNode()
