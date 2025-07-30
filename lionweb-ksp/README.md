# Module lionweb-ksp

This is a ksp processor (i.e., a lightweight compiler plugin) that find all AST classes and generate the Kotlin code
for the corresponding Language definition.

For example, it could generate something like this:

```
package com.strumenta.math

import com.strumenta.kolasu.lionweb.LanguageGeneratorCommand
import com.strumenta.kolasu.lionweb.KolasuLanguage
import com.strumenta.kolasu.lionweb.LionWebLanguageExporter
import io.lionweb.language.Language

val kLanguage = KolasuLanguage("com.strumenta.math").apply {
    addClass(Expression::class)
    addClass(BinaryExpression::class)
    addClass(SumExpression::class)
    addClass(SubExpression::class)
    addClass(IntLiteralExpr::class)
}

val lwLanguage: Language by lazy {
    val importer = LionWebLanguageExporter()
    importer.export(kLanguage)
}

fun main(args: Array<String>) {
    LanguageGeneratorCommand(lwLanguage).main(args)
}                    

```