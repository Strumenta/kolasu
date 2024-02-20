package com.strumenta.kolasu.semantics.scope.description

fun scope(
    ignoreCase: Boolean = false,
    init: ScopeDescription.() -> Unit
): ScopeDescription {
    return ScopeDescription(ignoreCase).apply(init)
}

class ScopeDescription(
    private val ignoreCase: Boolean = false
) {
    private var parent: ScopeDescription? = null
    private val entries: MutableMap<String, String> = mutableMapOf()

    fun resolve(name: String): String? {
        return this.entries[name.asKey()] ?: this.parent?.resolve(name)
    }

    fun define(
        name: String,
        identifier: String
    ) {
        this.entries[name] = identifier
    }

    fun parent(parent: ScopeDescription) {
        this.parent = parent
    }

    fun parent(
        ignoreCase: Boolean = false,
        init: ScopeDescription.() -> Unit
    ) {
        this.parent = scope(ignoreCase, init)
    }

    private fun String.asKey(): String {
        return if (ignoreCase) this.lowercase() else this
    }
}
