package com.strumenta.kolasu.semantics.symbol.provider

import com.strumenta.kolasu.model.Node
import kotlin.reflect.KClass

@SymbolProviderDsl
interface SymbolProviderConfigurationApi {
    fun <NodeTy : Node> symbolFor(
        nodeType: KClass<out NodeTy>,
        rule: SymbolProviderConfigurationRuleApi.(SymbolProviderConfigurationRuleContext<out NodeTy>) -> Unit
    )
}

@SymbolProviderDsl
interface SymbolProviderConfigurationRuleApi {
    fun include(name: String, value: Any?)
}
