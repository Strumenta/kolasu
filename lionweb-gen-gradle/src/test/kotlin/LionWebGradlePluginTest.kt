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
}