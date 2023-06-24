import com.strumenta.kolasu.lionwebgen.BuildConfig
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

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
                id("${BuildConfig.PLUGIN_ID}") version "${BuildConfig.PLUGIN_VERSION}"
            }
        """
        )

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("lionwebgen", "--stacktrace")
        runner.withProjectDir(projectDir)
        val result = runner.build()
    }

    @Test
    fun `generatePropertiesLanguage`() {
        projectDir!!.resolve("properties-language.json")
            .writeText(this.javaClass.getResourceAsStream("/properties-language.json").bufferedReader().readText())
        projectDir!!.resolve("build.gradle.kts").writeText(
            """plugins {
                id("${BuildConfig.PLUGIN_ID}") version "${BuildConfig.PLUGIN_VERSION}"
            }
            
            lionweb {
              packageName.set("com.strumenta.foo")
              languages.add(file("properties-language.json"))
            }
        """
        )

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("lionwebgen", "--stacktrace")
        runner.withProjectDir(projectDir)
        val result = runner.build()
    }

    @Test
    fun `compile code using AST`() {
        projectDir!!.resolve("properties-language.json")
            .writeText(this.javaClass.getResourceAsStream("/properties-language.json").bufferedReader().readText())
        File(projectDir, "src/main/kotlin").mkdirs()
        projectDir!!.resolve("src/main/kotlin/myfile.kt")
            .writeText("""
                import com.strumenta.foo.PropertiesFile
                
                val pf = PropertiesFile(mutableListOf())
            """.trimIndent())
        projectDir!!.resolve("build.gradle.kts").writeText(
            """plugins {
                id("org.jetbrains.kotlin.jvm") version "1.8.22"
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
              packageName.set("com.strumenta.foo")
              languages.add(file("properties-language.json"))
            }
            
            tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
                source(File(buildDir, "lionweb-gen"), sourceSets["main"].kotlin)
                dependsOn("lionwebgen")
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