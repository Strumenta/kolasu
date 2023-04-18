package com.strumenta.kolasu.language

import com.strumenta.kolasu.codegen.ASTCodeGenerator
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.parsing.ASTParser

/**
 * This permits to parse code into AST and viceversa going from an AST into code.
 */
class LanguageModule<R : Node>(val parser: ASTParser<R>, val codeGenerator: ASTCodeGenerator<R>)
