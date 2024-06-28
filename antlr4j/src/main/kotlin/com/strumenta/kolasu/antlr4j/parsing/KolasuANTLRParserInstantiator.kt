package com.strumenta.kolasu.antlr4j.parsing

import com.strumenta.kolasu.parsing.ASTParserInstantiator
import java.io.File

/**
 * This interface is used when we need to configure parsers. For example, it can be used for testing, when we need
 * to instantiate a parser with a particular configuration (e.g., specifying include lists of files).
 * With respect to [ASTParserInstantiator] this interface can be used to create more specific [KolasuParser] instances.
 */
interface KolasuANTLRParserInstantiator : ASTParserInstantiator {
    override fun instantiate(fileToParse: File): KolasuANTLRParser<*, *, *, *>

    override fun instantiate(code: String): KolasuANTLRParser<*, *, *, *>
}
