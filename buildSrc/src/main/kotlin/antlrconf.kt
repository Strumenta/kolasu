import org.gradle.api.Project
import org.gradle.kotlin.dsl.invoke

fun Project.setAntlrTasksDeps() {
    tasks {
        named("compileKotlin") {
            dependsOn("generateGrammarSource")
        }
        named("compileTestKotlin") {
            dependsOn("generateTestGrammarSource")
        }
        named("compileJava") {
            dependsOn("generateTestGrammarSource")
        }
        named("compileTestKotlin") {
            dependsOn("generateTestGrammarSource")
        }
        named("runKtlintCheckOverTestSourceSet") {
            dependsOn("generateTestGrammarSource")
        }
    }
}