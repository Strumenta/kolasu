package com.strumenta.kolasu.parsing

import com.strumenta.kolasu.model.Source
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.Charset

fun String.toStream(charset: Charset = Charsets.UTF_8) = ByteArrayInputStream(toByteArray(charset))

interface KolasuLexer<T : KolasuToken> {
    /**
     * Performs "lexing" on the given code string, i.e., it breaks it into tokens.
     */
    fun lex(
        code: String,
        onlyFromDefaultChannel: Boolean = true,
    ) = lex(code.byteInputStream(Charsets.UTF_8), Charsets.UTF_8, onlyFromDefaultChannel)

    /**
     * Performs "lexing" on the given code string, i.e., it breaks it into tokens.
     */
    fun lex(code: String) = lex(code, true)

    /**
     * Performs "lexing" on the given code stream, i.e., it breaks it into tokens.
     */
    fun lex(
        inputStream: InputStream,
        charset: Charset = Charsets.UTF_8,
        onlyFromDefaultChannel: Boolean = true,
        source: Source? = null,
    ): LexingResult<T>

    /**
     * Performs "lexing" on the given code stream, i.e., it breaks it into tokens.
     */
    fun lex(
        inputStream: InputStream,
        charset: Charset = Charsets.UTF_8,
    ) = lex(inputStream, charset, true)

    /**
     * Performs "lexing" on the given code stream, i.e., it breaks it into tokens.
     */
    fun lex(inputStream: InputStream) = lex(inputStream, Charsets.UTF_8, true)

    /**
     * Performs "lexing" on the given code stream, i.e., it breaks it into tokens.
     */
    fun lex(
        file: File,
        charset: Charset = Charsets.UTF_8,
        onlyFromDefaultChannel: Boolean = true,
    ): LexingResult<T> = BufferedInputStream(FileInputStream(file)).use { lex(it, charset, onlyFromDefaultChannel) }
}
