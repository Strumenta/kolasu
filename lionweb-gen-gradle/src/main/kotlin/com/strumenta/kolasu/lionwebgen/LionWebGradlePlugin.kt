package com.strumenta.kolasu.lionwebgen

import com.google.devtools.ksp.gradle.KspExtension
import com.strumenta.kolasu.lionweb.ASTGenerator
import com.strumenta.kolasu.lionweb.KotlinCodeProcessor
import com.strumenta.kolasu.lionweb.StarLasuLWLanguage
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.serialization.JsonSerialization
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.File
import java.io.FileInputStream

val tasksGroup = "lionweb"
val genASTClasses = "genASTClasses"
val genLanguages = "genLanguages"
val kspPlugin = "com.google.devtools.ksp"

class LionWebGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        if (!project.plugins.hasPlugin(kspPlugin)) {
            project.logger.warn("Please ensure to configure plugin \"$kspPlugin\"")
        }
        addDependencies(project)
        configureCompilation(project)

        val configuration = prepareConfiguration(project)
        configureKsp(project, configuration)
        createGenASTClassesTask(project, configuration)
        createGenLanguagesTask(project, configuration)
    }

    private fun createGenASTClassesTask(project: Project, configuration: LionWebGradleExtension) : Task {
        return project.tasks.create(genASTClasses) {
            it.group = tasksGroup
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

                            val ktFiles = ASTGenerator(configuration.importPackageNames.get()[language.name] ?: language.name, language)
                                .generateClasses(existingKotlinClasses)
                            ktFiles.forEach { ktFile ->
                                val file = File(configuration.outdir.get(), ktFile.path)
                                file.parentFile.mkdirs()
                                file.writeText(ktFile.code)
                                println("  generated ${file.path}")
                            }
                        }
                    }

                }
                println("LIonWeb AST Classes generation task - completed")
            }
        }
    }

    private fun createGenLanguagesTask(project: Project, configuration: LionWebGradleExtension) : Task {
        return project.tasks.create(genLanguages) { it ->
            it.group = tasksGroup
            it.dependsOn("compileKotlin")
            it.doLast {
                println("export packages: ${configuration.exportPackages.get()}")
                configuration.exportPackages.get().forEach { packageName ->
                    project.javaexec { jes ->
                        jes.classpath = project.sourceSets.getByName("main").runtimeClasspath
                        jes.mainClass.set("${packageName}.LanguageKt")
                        jes.args = mutableListOf(lionwebLanguageFile(project, packageName).absolutePath)
                    }
                }
            }
        }
    }

    /**
     * Prepare the plugin configuration setting in place default values.
     */
    private fun prepareConfiguration(project: Project) : LionWebGradleExtension {
        val configuration = project.extensions.create("lionweb", LionWebGradleExtension::class.java)
        configuration.outdir.convention(File(project.buildDir, "lionweb-gen"))
        val srcMainLionweb = project.file("src${File.separator}main${File.separator}lionweb")
        if (srcMainLionweb.exists() && srcMainLionweb.isDirectory) {
            configuration.languages.convention(
                srcMainLionweb.listFiles { _, name -> name != null && (name.endsWith(".json")) }?.toList()
                    ?: emptyList()
            )
        }
        configuration.exportPackages.convention(mutableListOf())
        return configuration
    }

    private fun lionwebLanguageFile(project: Project, packageName: String) : File {
        return File(project.buildDir, "lionwebgen${File.separator}$packageName.json")
    }

    /**
     * Configure KSP, so that it executes the KSP agent according to the configuration we specify.
     * In particular we communicate with KSP through a file (see kspFile). We use it to instruct the KSP agent about
     * paths to use.
     */
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

    /**
     * Configure the compilation to use the generated files.
     */
    private fun configureCompilation(project: Project) {
        project.sourceSets.getByName("main") {
            it.java.srcDir("src${File.separator}main${File.separator}java")
            it.java.srcDir(File(project.buildDir, "lionweb-gen"))
        }

        project.tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).forEach {
            it.source(File(project.buildDir, "lionweb-gen"),
                File(project.rootDir, "src${File.separator}main${File.separator}kotlin"))
            it.dependsOn(genASTClasses)
        }
    }

    private fun addDependencies(project: Project) {
        fun addKolasuModule(moduleName: String) {
            project.dependencies.add("implementation",
                "com.strumenta.kolasu:kolasu-$moduleName:${project.kolasuVersion}")
        }

        addKolasuModule("core")
        addKolasuModule("lionweb-ksp")
        addKolasuModule("lionweb")
        addKolasuModule("lionweb-gen")
        project.dependencies.add("ksp", "com.strumenta.kolasu:kolasu-lionweb-ksp:${project.kolasuVersion}")
        project.dependencies.add("implementation", "com.github.ajalt.clikt:clikt:3.5.0")

        // We need to use this one to avoid an issue with Gson
        project.dependencies.add("implementation", "io.lionweb.lioncore-java:lioncore-java-core-fat:${project.lionwebVersion}")
    }

}