package com.strumenta.starlasu2

import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.serialization.SerializationProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.File

val tasksGroup = "starlasu"
val genASTClasses = "generateASTClasses"

class LWCorePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val configuration = prepareConfiguration(project)
        createGenASTClassesTask(project, configuration)
    }

    /**
     * Prepare the plugin configuration setting in place default values.
     */
    private fun prepareConfiguration(project: Project) : LWCoreGradleExtension {
        val configuration = project.extensions.create("starlasu2", LWCoreGradleExtension::class.java)
        return configuration
    }

    private fun createGenASTClassesTask(project: Project, configuration: LWCoreGradleExtension) : Task {
        return project.tasks.create(genASTClasses) {
            this.group = tasksGroup
            this.description = "Generate Kolasu ASTs from LionWeb languages"
            //this.inputs.files(emptyList<Any>())
            //this.outputs.dir(emptyList<Any>())
            this.doLast {
                println("LIonWeb AST Classes generation task - started")
                val starLasuDir = File(File(File(project.projectDir,"src"), "main"), "starlasu")
                if (starLasuDir.exists() && starLasuDir.isDirectory) {
                    starLasuDir.listFiles().forEach { file ->
                        println("Process file: ${file.absolutePath}")
                        generateASTClassesForLanguage(project, configuration, file)
                    }
                }
                println("LIonWeb AST Classes generation task - completed")
            }
        }
    }

    private fun generateASTClassesForLanguage(project: Project, configuration: LWCoreGradleExtension, languageFile: File) {
        val jsonSerialization = SerializationProvider.getStandardJsonSerialization()
        val language = jsonSerialization.deserializeToNodes(languageFile).filterIsInstance<Language>().first()
        println("Language: ${language.name}")
    }

}