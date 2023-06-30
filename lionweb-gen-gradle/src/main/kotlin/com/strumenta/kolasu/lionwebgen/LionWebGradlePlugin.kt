package com.strumenta.kolasu.lionwebgen

import com.strumenta.kolasu.lionweb.ASTGenerator
import com.strumenta.kolasu.lionweb.KotlinCodeProcessor
import com.strumenta.kolasu.lionweb.StarLasuLWLanguage
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.serialization.JsonSerialization
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.JavaExec
import org.gradle.configurationcache.extensions.capitalized
import java.io.File
import java.io.FileInputStream
import com.google.devtools.ksp.gradle.KspExtension

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

                            val ktFiles = ASTGenerator(configuration.importPackageNames.get()[language.name] ?: language.name, language).generateClasses(existingKotlinClasses)
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
        return File(project.buildDir, "lionwebgen${File.separator}$packageName.json")
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
        val srcMainLionweb = project.file("src${File.separator}main${File.separator}lionweb")
        if (srcMainLionweb.exists() && srcMainLionweb.isDirectory) {
            configuration.languages.convention(
                srcMainLionweb.listFiles { _, name -> name != null && (name.endsWith(".json") || name.endsWith(".ecore")) }?.toList() ?: emptyList()
            )
        }
        configuration.exportPackages.convention(mutableListOf())
        return configuration
    }

    override fun apply(project: Project) {
        if (!project.plugins.hasPlugin("com.google.devtools.ksp")) {
            project.logger.warn("Please ensure to configure plugin \"com.google.devtools.ksp\"")
        }
        addDependencies(project)
        configureCompilation(project)


        val configuration = prepareConfiguration(project)
        configureKsp(project, configuration)
        createGenASTClasses(project, configuration)
        createGenLanguages(project, configuration)
    }

    private fun configureKsp(project: Project, configuration: LionWebGradleExtension) {
        val ksp = project.extensions.findByName("ksp") as KspExtension
        val kspFile = File(project.buildDir, "lionwebgen${File.separator}kspconf.txt")
        ksp.arg("lionwebgendir", File(project.buildDir, "lionwebgen").absolutePath)
        ksp.arg("file", kspFile.absolutePath)

        val prepareKsp = project.tasks.create("prepareKsp") {
            it.doFirst {
                val exportPackagesStr = configuration.exportPackages.get().let { it.joinToString(",") }
                kspFile.parentFile.mkdirs()
                kspFile.writeText("exportPackages=$exportPackagesStr\n")
            }
        }
        project.tasks.getByName("compileKotlin").dependsOn(prepareKsp)
    }

    private fun configureCompilation(project: Project) {
        project.sourceSets.getByName("main") {
            it.java.srcDir("src${File.separator}main${File.separator}java")
            it.java.srcDir(File(project.buildDir, "lionweb-gen"))
        }

        project.tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).forEach {
            it.source(File(project.buildDir, "lionweb-gen"), File(project.rootDir, "src/main/kotlin"))
            it.dependsOn("genASTClasses")
        }
    }

    private fun addDependencies(project: Project) {
        project.dependencies.add("implementation", "com.strumenta.kolasu:kolasu-core:${project.kolasuVersion}")
        project.dependencies.add("implementation", "com.strumenta.kolasu:kolasu-lionweb-ksp:${project.kolasuVersion}")
        project.dependencies.add("implementation", "com.strumenta.kolasu:kolasu-lionweb:${project.kolasuVersion}")
        project.dependencies.add("implementation", "com.strumenta.kolasu:kolasu-lionweb-gen:${project.kolasuVersion}")
        project.dependencies.add("ksp", "com.strumenta.kolasu:kolasu-lionweb-ksp:${project.kolasuVersion}")
        project.dependencies.add("implementation", "com.github.ajalt.clikt:clikt:3.5.0")
        project.dependencies.add("implementation", "io.lionweb.lioncore-java:lioncore-java-core-fat:${project.lionwebVersion}")
    }

}