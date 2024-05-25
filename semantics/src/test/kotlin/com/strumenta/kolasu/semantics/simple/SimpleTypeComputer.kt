package com.strumenta.kolasu.semantics.simple

import com.strumenta.kolasu.semantics.typing.typeComputer

val simpleTypeComputer = typeComputer {

    typeFor(TypeDecl::class) { it }

    typeFor(OperationDecl::class) { operationDecl ->
        simpleReferenceResolver.resolve(operationDecl, OperationDecl::returns)?.let(this::typeFor)
    }

    typeFor(FeatureDecl::class) { featureDecl ->
        simpleReferenceResolver.resolve(featureDecl, FeatureDecl::type)?.let(this::typeFor)
    }

    typeFor(RefExpr::class) { refExpr ->
        simpleReferenceResolver.resolve(refExpr, RefExpr::symbol)?.let(this::typeFor)
    }

    typeFor(CallExpr::class) { callExpr ->
        simpleReferenceResolver.resolve(callExpr, CallExpr::operation)?.let(this::typeFor)
    }
}
