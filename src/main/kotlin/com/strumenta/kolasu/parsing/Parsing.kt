package com.strumenta.kolasu.parsing

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.validation.Error
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.Charset

data class ParsingResult<RootNode : Node>(val root: RootNode?, val errors: List<Error>, val code: String, val incompleteNode: Node? = null) {
    fun isCorrect() = errors.isEmpty() && root != null
}

fun String.toStream(charset: Charset = Charsets.UTF_8) = ByteArrayInputStream(toByteArray(charset))

interface Parser<RootNode : Node> {
    fun parse(code: String, withValidation: Boolean = true): ParsingResult<RootNode> = parse(code.toStream(), withValidation)

    fun parse(file: File, withValidation: Boolean = true): ParsingResult<RootNode> = parse(FileInputStream(file), withValidation)

    fun parse(inputStream: InputStream, withValidation: Boolean = true): ParsingResult<RootNode>
}
