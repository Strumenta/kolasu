@file:OptIn(ExperimentalCompilerApi::class)

package com.strumenta.kolasu.kcp

import com.tschuchort.compiletesting.CompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import java.io.File
import java.net.URLClassLoader

@ExperimentalCompilerApi
fun compile(
    sourceFiles: List<SourceFile>,
    plugin: CompilerPluginRegistrar = StarLasuComponentRegistrar(),
): CompilationResultWithClassLoader {
    val kotlinCompilation =
        KotlinCompilation().apply {
            sources = sourceFiles
            // useIR = true
            compilerPluginRegistrars = listOf(plugin)
            inheritClassPath = true
        }
    val result = kotlinCompilation.compile()
    return CompilationResultWithClassLoader(result, kotlinCompilation.classpaths, kotlinCompilation.classesDir)
}

data class CompilationResultWithClassLoader(
    val compilationResult: CompilationResult,
    val classpaths: kotlin.collections.List<java.io.File>,
    val outputDirectory: File,
) {
    val messages: String
        get() = compilationResult.messages
    val exitCode: KotlinCompilation.ExitCode
        get() = compilationResult.exitCode

    // It is important to REUSE the classloader and not re-create it
    val classLoader =
        URLClassLoader(
            // Include the original classpaths and the output directory to be able to load classes from dependencies.
            classpaths.plus(outputDirectory).map { it.toURI().toURL() }.toTypedArray(),
            this::class.java.classLoader,
        )
}

fun compile(
    sourceFile: SourceFile,
    plugin: CompilerPluginRegistrar = StarLasuComponentRegistrar(),
): CompilationResultWithClassLoader = compile(listOf(sourceFile), plugin)

fun CompilationResultWithClassLoader.assertHasMessage(regex: Regex) {
    val messageLines = this.messages.lines()
    assert(messageLines.any { regex.matches(it) }) {
        "No message line found matching $regex. Message lines found:\n - ${messageLines.joinToString("\n - ")}"
    }
}

fun CompilationResultWithClassLoader.assertHasMessage(msg: String) {
    val messageLines = this.messages.lines()
    assert(messageLines.any { msg == it })
}

fun CompilationResultWithClassLoader.assertHasNotMessage(regex: Regex) {
    val messageLines = this.messages.lines()
    assert(messageLines.none { regex.matches(it) })
}
