package com.strumenta.starlasuv2

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*

class MyProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    private val created = mutableSetOf<String>()
    override fun process(resolver: Resolver): List<KSAnnotated> {
        println("PROCESSING")
        resolver.getAllFiles().forEach { file ->
            file.declarations.forEach { declaration ->
                val toGen = declaration.annotations.any { annotation ->
                    annotation.annotationType.resolve().declaration.qualifiedName!!.asString() == StarLasuGen::class.qualifiedName
                }
                if (toGen && declaration is KSClassDeclaration) {
                    logger.warn("Class ${declaration.simpleName.asString()} is annotated with @StarLasuGen")
                    val fileName = "${declaration.simpleName.asString()}CodeModelClasses"
                    logger.warn("GENERATED: ${created.joinToString(", ")}")
                    if (fileName !in created) {
                        created.add(fileName)
                        val file = codeGenerator.createNewFile(
                            Dependencies.ALL_FILES,
                            declaration.packageName.asString(),
                            fileName
                        )
                        file.writer().use {
                            it.write(
                                """
                    ${
                                    if (declaration.packageName.asString()
                                            .isNotBlank()
                                    ) "package ${declaration.packageName.asString()}" else ""
                                }                            
                    
                    class $fileName {
                        fun greet() = "Hello!"
                    }
                    """.trimIndent()
                            )
                        }
                    }
                }
            }
        }
//        val symbols = resolver.getSymbolsWithAnnotation(StarLasuGen::class.qualifiedName!!)
//        println("SYMBOLS: ${symbols.count()}")
//
//        symbols.forEach { symbol ->
//            if (symbol is KSClassDeclaration) {
//                val annotation = symbol.annotations.first { it.shortName.asString() == "MyAnnotation" }
//                val name = annotation.arguments.first().value as String
//
//                val fileName = "${symbol.simpleName.asString()}Generated"
//                val file = codeGenerator.createNewFile(
//                    Dependencies.ALL_FILES,
//                    symbol.packageName.asString(),
//                    fileName
//                )
//                file.writer().use {
//                    it.write(
//                        """
//                    package ${symbol.packageName.asString()}
//
//                    class $fileName {
//                        fun greet() = "Hello, $name!"
//                    }
//                    """.trimIndent()
//                    )
//                }
//            }
//        }

        return emptyList()
    }
}

class MyProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return MyProcessor(environment.codeGenerator, environment.logger)
    }
}