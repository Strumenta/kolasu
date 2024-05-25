package com.strumenta.kolasu.semantics.simple

import com.strumenta.kolasu.semantics.scoping.ScopeComputer
import com.strumenta.kolasu.semantics.scoping.scope
import com.strumenta.kolasu.semantics.scoping.scopeComputer
import com.strumenta.kolasu.traversing.findAncestorOfType

val simpleScopeComputer: ScopeComputer = scopeComputer {
    scopeFrom(CompilationUnit::class) { compilationUnit ->
        scope { compilationUnit.content.forEach { this.define(it.name, it) } }
    }
    scopeFrom(ClassDecl::class) { classDecl ->
        scope {
            classDecl.features.forEach { define(it.name, it) }
            classDecl.operations.forEach { define(it.name, it) }
            simpleReferenceResolver.resolve(classDecl, ClassDecl::superclass)
                ?.let { parents += scopeFrom(it) }
            classDecl.findAncestorOfType(CompilationUnit::class.java)
                ?.let { parents += scopeFrom(it) }
        }
    }
    scopeFrom(StmtNode::class) { stmt ->
        when (stmt) {
            is DeclarationStmt -> scope {
                define(stmt.variable.name, stmt.variable)
                stmt.findAncestorOfType(ClassDecl::class.java)
                    ?.let { parents += scopeFrom(it) }
            }
            else -> stmt.findAncestorOfType(ClassDecl::class.java)?.let { scopeFrom(it) }
        }
    }
}
