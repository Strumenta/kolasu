plugins {
    id("antlr")
    id("maven-publish")
    id("signing")
}

val kotlinVersion = extra["kotlinVersion"]

dependencies {
    antlr(libs.antlr4Tool)
    implementation(libs.antlr4jRuntime)
    implementation(project(":ast"))
    implementation(project(":core"))
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
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
