package com.strumenta.kolasu.language

sealed class Type(
    open val name: String,
)

sealed class DataType(
    name: String,
) : Type(name)

data class PrimitiveType(
    override val name: String,
) : DataType(name) {
    companion object {
        private val cache = mutableMapOf<String, PrimitiveType>()

        fun get(name: String): PrimitiveType {
            if (!cache.containsKey(name)) {
                cache[name] = PrimitiveType(name)
            }
            return cache[name]!!
        }
    }
}

data class EnumType(
    override val name: String,
    val literals: MutableList<EnumerationLiteral> = mutableListOf(),
) : DataType(name)

data class EnumerationLiteral(
    val name: String,
)
