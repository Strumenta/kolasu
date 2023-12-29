plugins {
    id("antlr")
    id("maven-publish")
    id("signing")
}

val antlrVersion = extra["antlr_version"]
val kotlinVersion = extra["kotlinVersion"]

dependencies {
    antlr("org.antlr:antlr4:$antlrVersion")
    implementation("org.antlr:antlr4-runtime:$antlrVersion")
    implementation(project(":core"))
}

fun Project.useAntlrInTests(packageName: String) {
    tasks.generateTestGrammarSource {
        maxHeapSize = "64m"
        arguments = arguments + listOf("-package", packageName)
        outputDirectory = File("generated-test-src/antlr/main/${packageName.replace('.', '/')}".toString())
    }
    sourceSets.getByName("test") {
        java.srcDir("src/test/java")
        java.srcDir("generated-test-src/antlr/main")
    }
    setAntlrTasksDeps()
}

project.useAntlrInTests("com.strumenta.simplelang")

tasks.clean {
    delete("generated-src")
    delete("generated-test-src")
}

publishing {
    addSonatypeRepo(project)
    addPublication("kolasu_antlr", "ANTLR integration for Kolasu", project)
}

signing {
    sign(publishing.publications["kolasu_antlr"])
}

tasks.named("sourcesJar") {
    dependsOn("generateGrammarSource")
}

tasks.named("runKtlintCheckOverMainSourceSet") {
    dependsOn("generateGrammarSource")
}

tasks.named("runKtlintCheckOverTestSourceSet") {
    dependsOn("generateTestGrammarSource")
}

tasks.named("runKtlintFormatOverMainSourceSet") {
    dependsOn("generateGrammarSource")
}

tasks.named("runKtlintFormatOverTestSourceSet") {
    dependsOn("generateTestGrammarSource")
}
