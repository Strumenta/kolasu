plugins {
    id("antlr")
}

val antlr_version = extra["antlr_version"]
val kotlin_version = extra["kotlin_version"]
val isReleaseVersion = !(version as String).endsWith("SNAPSHOT")

dependencies {
    antlr("org.antlr:antlr4:$antlr_version")
    implementation("org.antlr:antlr4-runtime:$antlr_version")
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
