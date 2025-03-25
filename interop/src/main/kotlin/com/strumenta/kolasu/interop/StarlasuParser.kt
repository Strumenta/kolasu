package com.strumenta.kolasu.interop

import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.serialization.PrimitiveValuesSerialization

/**
 * In the future this interface should use more concrete types.
 * Right now, to maximize stability, we exchange strings in LionWeb Format.
 */
interface StarlasuParser {

    fun languageName() : String

    fun extensions() : Set<String>

    /**
     * Return the language used, in LionWeb Format
     */
    fun languages() : List<Language>

    fun preparePrimitiveSerialization(primitiveSerialization: PrimitiveValuesSerialization)

    /**
     * Produce a Parsing Result serialized according to the LionWeb Specifications.
     */
    fun parse(codebaseName: String, relativePath: String, code: String) : String
}