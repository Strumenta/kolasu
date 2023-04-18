package com.strumenta.kolasu.parsing

import com.strumenta.kolasu.model.Node
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset

private fun inputStreamToString(inputStream: InputStream, charset: Charset = Charsets.UTF_8): String =
    inputStream.bufferedReader(charset).use(BufferedReader::readText)

interface ASTParser<R : Node> {
    fun parse(
        inputStream: InputStream,
        charset: Charset = Charsets.UTF_8,
        considerRange: Boolean = true,
        measureLexingTime: Boolean = false
    ):
        ParsingResult<R> = parse(inputStreamToString(inputStream, charset), considerRange, measureLexingTime)
    fun parse(code: String, considerRange: Boolean = true, measureLexingTime: Boolean = false): ParsingResult<R>

    fun parse(file: File, charset: Charset = Charsets.UTF_8, considerRange: Boolean = true): ParsingResult<R>
}
