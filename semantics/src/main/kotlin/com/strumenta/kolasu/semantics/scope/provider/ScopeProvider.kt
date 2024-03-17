package com.strumenta.kolasu.semantics.scope.provider

import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceValue
import com.strumenta.kolasu.semantics.scope.description.ScopeDescription
import kotlin.reflect.KProperty1

interface ScopeProvider {
    fun <NodeType : NodeLike> scopeFor(
        node: NodeType,
        reference: KProperty1<in NodeType, ReferenceValue<out PossiblyNamed>?>,
    ): ScopeDescription
}
