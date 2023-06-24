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

    @Test()
    fun `no configuration`() {
        projectDir!!.resolve("build.gradle.kts").writeText(
            """plugins {
                id("com.strumenta.kolasu.lionwebgenu")
            }
        """
        )


        // Run the build
       // try {
            val runner = GradleRunner.create()
            runner.forwardOutput()
            runner.withPluginClasspath()
            runner.withArguments("starlasuCheck", "--stacktrace")
            runner.withProjectDir(projectDir)
            val result = runner.build()
//            fail()
//        } catch (e: GradleException) {
//            assertEquals("Validation failed", e.message)
//        }
    }
}