package com.strumenta.kolasu.semantics.simple

import com.strumenta.kolasu.semantics.linking.ReferenceResolver
import com.strumenta.kolasu.semantics.linking.referenceResolver
import com.strumenta.kolasu.traversing.findAncestorOfType

val simpleReferenceResolver: ReferenceResolver = referenceResolver {
    resolve(ClassDecl::superclass) { (classDecl, superclass) ->
        classDecl.findAncestorOfType(CompilationUnit::class.java)
            ?.let { simpleScopeComputer.scopeFrom(it) }
            ?.entries<ClassDecl> { (name) -> name == superclass }?.firstOrNull()
    }
    resolve(FeatureDecl::type) { (featureDecl, type) ->
        featureDecl.findAncestorOfType(CompilationUnit::class.java)
            ?.let { simpleScopeComputer.scopeFrom(it) }
            ?.entries<TypeDecl> { (name) -> name == type }?.firstOrNull()
    }
    resolve(OperationDecl::returns) { (operationDecl, returns) ->
        operationDecl.findAncestorOfType(CompilationUnit::class.java)
            ?.let { simpleScopeComputer.scopeFrom(it) }
            ?.entries<TypeDecl> { (name) -> name == returns }?.firstOrNull()
    }
    resolve(ParameterDecl::type) { (parameterDecl, type) ->
        parameterDecl.findAncestorOfType(CompilationUnit::class.java)
            ?.let { simpleScopeComputer.scopeFrom(it) }
            ?.entries<TypeDecl> { (name) -> name == type }?.firstOrNull()
    }
    resolve(Variable::type) { (variable, type) ->
        variable.findAncestorOfType(CompilationUnit::class.java)
            ?.let { simpleScopeComputer.scopeFrom(it) }
            ?.entries<TypeDecl> { (name) -> name == type }?.firstOrNull()
    }
    resolve(RefExpr::symbol) { (refExpr, symbol) ->
        when (val context = refExpr.context) {
            null -> refExpr.findAncestorOfType(StmtNode::class.java)
            else -> simpleTypeComputer.typeFor(context)
        }?.let { simpleScopeComputer.scopeFrom(it) }
            ?.entries<SymbolNode> { (name) -> name == symbol }?.firstOrNull()
    }
    resolve(CallExpr::operation) { (callExpr, operation) ->
        callExpr.findAncestorOfType(ClassDecl::class.java)
            ?.let { simpleScopeComputer.scopeFrom(it) }
            ?.entries<OperationDecl> { (name) -> name == operation }?.firstOrNull()
    }
}
