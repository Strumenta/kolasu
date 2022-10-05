package com.strumenta.kolasu.language

import com.strumenta.kolasu.codegen.ASTCodeGenerator
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.parsing.KolasuParser
import com.strumenta.kolasu.traversing.walkChildren

class LanguageModule<R : Node>(val parser: KolasuParser<R, *, *>, val codeGenerator: ASTCodeGenerator<R>)
