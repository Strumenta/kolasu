package com.strumenta.starlasu2

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

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
        val configuration = project.extensions.create("starlasu", LWCoreGradleExtension::class.java)
        return configuration
    }

    private fun createGenASTClassesTask(project: Project, configuration: LWCoreGradleExtension) : Task {
        return project.tasks.create(genASTClasses) {
            this.group = tasksGroup
            this.description = "Generate Kolasu ASTs from LionWeb languages"
            this.inputs.files(emptyList<Any>())
            this.outputs.dir(emptyList<Any>())
            this.doLast {
                println("LIonWeb AST Classes generation task - started")
                println("Exploring: ${project.rootDir.absolutePath}")
                println("LIonWeb AST Classes generation task - completed")
            }
        }
    }

}