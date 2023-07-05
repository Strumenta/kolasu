import com.strumenta.kolasu.lionwebgen.BuildConfig
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.Ignore


class LionWebGradlePluginTest {
    var projectDir: File? = null

    @BeforeEach
    fun setup() {
        projectDir = Files.createTempDirectory("myBuildTest").toFile()
        projectDir!!.mkdirs()
    }

    @Test
    fun `no configuration`() {
        projectDir!!.resolve("build.gradle.kts").writeText(
            """plugins {
                id("org.jetbrains.kotlin.jvm") version "1.8.22"
                id("com.google.devtools.ksp") version "1.8.22-1.0.11"
                id("${BuildConfig.PLUGIN_ID}") version "${BuildConfig.PLUGIN_VERSION}"
            }
        """
        )

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("genASTClasses", "--stacktrace")
        runner.withProjectDir(projectDir)
        val result = runner.build()
    }

    @Test
    fun `generatePropertiesLanguage`() {
        projectDir!!.resolve("properties-language.json")
            .writeText(this.javaClass.getResourceAsStream("/properties-language.json").bufferedReader().readText())
        projectDir!!.resolve("build.gradle.kts").writeText(
            """plugins {
                id("org.jetbrains.kotlin.jvm") version "1.8.22"
                id("com.google.devtools.ksp") version "1.8.22-1.0.11"                
                id("${BuildConfig.PLUGIN_ID}") version "${BuildConfig.PLUGIN_VERSION}"
            }
            
            lionweb {
              importPackageNames.set(mutableMapOf("io.lionweb.Properties" to "com.strumenta.foo"))
              languages.add(file("properties-language.json"))
            }
        """
        )

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("genASTClasses", "--stacktrace")
        runner.withProjectDir(projectDir)
        val result = runner.build()
    }

    @Test
    @Ignore
    fun `compile code using AST`() {
        projectDir!!.resolve("properties-language.json")
            .writeText(this.javaClass.getResourceAsStream("/properties-language.json").bufferedReader().readText())
        File(projectDir, "src/main/kotlin").mkdirs()
        projectDir!!.resolve("gradle.properties").writeText("""
kolasuVersion=${BuildConfig.PLUGIN_VERSION}
        """)
        projectDir!!.resolve("src/main/kotlin/myfile.kt")
            .writeText("""
                import com.strumenta.foo.PropertiesFile
                
                val pf = PropertiesFile(mutableListOf())
            """.trimIndent())
        projectDir!!.resolve("build.gradle.kts").writeText(
            """plugins {
                id("org.jetbrains.kotlin.jvm") version "1.8.22"
                id("com.google.devtools.ksp") version "1.8.22-1.0.11"                
                id("${BuildConfig.PLUGIN_ID}") version "${BuildConfig.PLUGIN_VERSION}"
            }
           
           repositories {
              mavenLocal()
              mavenCentral()
           }
           
           dependencies {
             implementation("com.strumenta.kolasu:kolasu-core:${BuildConfig.PLUGIN_VERSION}")
           }
            
            lionweb {
              importPackageNames.set(mutableMapOf("io.lionweb.Properties" to "com.strumenta.foo"))
              languages.add(file("properties-language.json"))
            }
            
            tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
                source(File(buildDir, "lionweb-gen"), sourceSets["main"].kotlin)
                dependsOn("genASTClasses")
            }
        """
        )

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("compileKotlin", "--stacktrace")
        runner.withProjectDir(projectDir)
        val result = runner.build()
    }

    @Test
    @Ignore
    fun `pick it up from src main lionweb automatically`() {
        File(projectDir, "src/main/kotlin").mkdirs()
        File(projectDir, "src/main/lionweb").mkdirs()
        projectDir!!.resolve("src/main/lionweb/properties-language.json")
            .writeText(this.javaClass.getResourceAsStream("/properties-language.json").bufferedReader().readText())
        projectDir!!.resolve("src/main/kotlin/myfile.kt")
            .writeText("""
                import com.strumenta.foo.PropertiesFile
                
                val pf = PropertiesFile(mutableListOf())
            """.trimIndent())
        projectDir!!.resolve("build.gradle.kts").writeText(
            """plugins {
                id("org.jetbrains.kotlin.jvm") version "1.8.22"
                id("com.google.devtools.ksp") version "1.8.22-1.0.11"                
                id("${BuildConfig.PLUGIN_ID}") version "${BuildConfig.PLUGIN_VERSION}"
            }
           
           repositories {
              mavenLocal()
              mavenCentral()
           }
           
           dependencies {
             implementation("com.strumenta.kolasu:kolasu-core:${BuildConfig.PLUGIN_VERSION}")
           }
            
            lionweb {
              importPackageNames.set(mutableMapOf("io.lionweb.Properties" to "com.strumenta.foo"))
            }
            
            tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
                source(File(buildDir, "lionweb-gen"), sourceSets["main"].kotlin)
                dependsOn("genASTClasses")
            }
        """
        )

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("compileKotlin", "--stacktrace")
        runner.withProjectDir(projectDir)
        val result = runner.build()
    }

    @Test
    @Ignore
    fun `try out the exporter`() {
        File(projectDir, "src/main/kotlin").mkdirs()
        projectDir!!.resolve("src/main/kotlin/myfile.kt")
            .writeText("""
                import com.strumenta.kolasu.model.Node
                
                data class A(val p1: String): Node
            """.trimIndent())
        projectDir!!.resolve("build.gradle.kts").writeText(
            """plugins {
                id("org.jetbrains.kotlin.jvm") version "1.8.22"
                id("com.google.devtools.ksp") version "1.8.22-1.0.11"
                id("${BuildConfig.PLUGIN_ID}") version "${BuildConfig.PLUGIN_VERSION}"
            }
           
           repositories {
              mavenLocal()
              mavenCentral()
              maven(url="https://s01.oss.sonatype.org/content/repositories/snapshots/")
           }
           
           dependencies {
             implementation("com.strumenta.kolasu:kolasu-core:${BuildConfig.PLUGIN_VERSION}")
             implementation("com.strumenta.kolasu:kolasu-lionweb-gen:${BuildConfig.PLUGIN_VERSION}")
             ksp("com.strumenta.kolasu:kolasu-lionweb-gen:${BuildConfig.PLUGIN_VERSION}")
           }
            
        """
        )

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("compileKotlin", "--stacktrace")
        runner.withProjectDir(projectDir)
        val result = runner.build()
    }
}