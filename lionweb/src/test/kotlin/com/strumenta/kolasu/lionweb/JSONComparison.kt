package com.strumenta.kolasu.lionweb

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import kotlin.test.assertEquals
import kotlin.test.fail

fun assertJSONsAreEqual(
    expected: String,
    actual: String,
    context: String = "<ROOT>",
) {
    assertJSONsAreEqual(JsonParser.parseString(expected), JsonParser.parseString(actual), context)
}

fun assertJSONsAreEqual(
    expected: JsonElement,
    actual: JsonElement,
    context: String = "<ROOT>",
) {
    when{
        expected is JsonObject && actual is JsonObject -> {
            assertEquals(expected.keySet(), actual.keySet(),
                "Json Object keys do not match at $context")
            expected.keySet().forEach { key ->
                assertJSONsAreEqual(expected[key], actual[key], "$context.$key")
            }
        }
        expected is JsonArray && actual is JsonArray -> {
            assertEquals(
                expected.size(),
                actual.size(),
                "Json Array size mismatch at $context. Expected: ${expected.size()}, Actual: ${actual.size()}"
            )
            (0 until expected.size()).forEach { i ->
                assertJSONsAreEqual(expected[i], actual[i], "$context[$i]")
            }
        }
        expected is JsonPrimitive && actual is JsonPrimitive -> {
            assertEquals(expected, actual,
                "Json Primitive value mismatch at $context")
        }
        else -> {
            fail("Json Type mismatch at $context. Expected: ${expected::class.simpleName}, actual: ${actual::class.simpleName}")
        }
    }
}
