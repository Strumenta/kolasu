package com.strumenta.kolasu.lionwebgen

import com.strumenta.kolasu.lionweb.ASTGenerator
import com.strumenta.kolasu.lionweb.KotlinCodeProcessor
import com.strumenta.kolasu.lionweb.StarLasuLWLanguage
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.serialization.JsonSerialization
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.process.JavaExecSpec
import org.gradle.process.internal.DefaultExecActionFactory
import org.gradle.process.internal.DefaultJavaExecAction
import java.io.File
import java.io.FileInputStream

val Project.sourceSets: SourceSetContainer
    get() = (this as ExtensionAware).extensions.getByName("sourceSets") as SourceSetContainer


class LionWebGradlePlugin : Plugin<Project> {

    fun createGenASTClasses(project: Project, configuration: LionWebGradleExtension) : Task {
        return project.tasks.create("genASTClasses") {
            it.group = "lionweb"
            it.doLast {
                println("LIonWeb AST Classes generation task - started")
                println("  languages: ${configuration.languages.get()}")
                configuration.languages.get().forEach { languageFile ->
                    println("prcessing languageFile $languageFile")
                    when (languageFile.extension) {
                        "json" -> {
                            val jsonser = JsonSerialization.getStandardSerialization()
                            jsonser.nodeResolver.addTree(StarLasuLWLanguage)
                            val language = jsonser.unserializeToNodes(FileInputStream(languageFile)).first() as Language
                            val existingKotlinClasses = KotlinCodeProcessor().classesDeclaredInDir(project.file("src/main/kotlin"))

                            val ktFiles = ASTGenerator(configuration.packageName.get(), language).generateClasses(existingKotlinClasses)
                            ktFiles.forEach { ktFile ->
                                val file = File(configuration.outdir.get(), ktFile.path)
                                file.parentFile.mkdirs()
                                file.writeText(ktFile.code)
                                println("  generated ${file.path}")
                            }
                        }
                        "ecore" -> {
                            throw RuntimeException("Working on ecore files")
                        }
                    }

                }
                println("LIonWeb AST Classes generation task - completed")
            }
        }
    }

    private fun languageFile(project: Project, packageName: String) : File {
        return File(project.buildDir, "lionwebgen/$packageName.json")
    }

    private fun createGenLanguage(project: Project,
                                  configuration: LionWebGradleExtension,
                                  packageName: String) : Task {
        return project.tasks.create("genLanguage${packageName.capitalized().replace('.', '_')}",
            JavaExec::class.java) {
            it.group = "lionweb"
            it.dependsOn("compileKotlin")
            it.classpath = project.sourceSets.getByName("main").runtimeClasspath
            it.mainClass.set("com.strumenta.props.LanguageKt")
            it.args =mutableListOf(languageFile(project, packageName).absolutePath)

        }
    }

    fun createGenLanguages(project: Project, configuration: LionWebGradleExtension) : Task {
        return project.tasks.create("genLanguages") { it ->
            it.group = "lionweb"
            it.dependsOn("compileKotlin")
            it.doLast {
                println("export packages: ${configuration.exportPackages.get()}")
                configuration.exportPackages.get().forEach { packageName ->
                    project.javaexec { jes ->
                        jes.classpath = project.sourceSets.getByName("main").runtimeClasspath
                        jes.mainClass.set("${packageName}.LanguageKt")
                        jes.args = mutableListOf(languageFile(project, packageName).absolutePath)
                    }
                }
            }
        }
    }

    fun prepareConfiguration(project: Project) : LionWebGradleExtension {
        val configuration = project.extensions.create("lionweb", LionWebGradleExtension::class.java)
        configuration.outdir.convention(File(project.buildDir, "lionweb-gen"))
        val srcMainLionweb = project.file("src/main/lionweb")
        if (srcMainLionweb.exists() && srcMainLionweb.isDirectory) {
            configuration.languages.convention(
                srcMainLionweb.listFiles { _, name -> name != null && (name.endsWith(".json") || name.endsWith(".ecore")) }?.toList() ?: emptyList()
            )
        }
        configuration.exportPackages.convention(mutableListOf())
        return configuration
    }

    override fun apply(project: Project) {
        val configuration = prepareConfiguration(project)
        createGenASTClasses(project, configuration)
        createGenLanguages(project, configuration)
    }

}