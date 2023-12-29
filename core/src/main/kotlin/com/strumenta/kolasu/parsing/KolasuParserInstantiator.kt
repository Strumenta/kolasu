package com.strumenta.kolasu.parsing

import java.io.File

/**
 * This interface is used when we need to configure parsers. For example, it can be used for testing, when we need
 * to instantiate a parser with a particular configuration (e.g., specifying include lists of files).
 */
interface KolasuParserInstantiator {
    fun instantiate(fileToParse: File): KolasuParser<*, *, *, *>
    fun instantiate(code: String): KolasuParser<*, *, *, *>
}
