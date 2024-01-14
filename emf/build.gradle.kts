plugins {
    id("antlr")
}

val antlrVersion = extra["antlr_version"]
val kotlinVersion = extra["kotlinVersion"]
val gsonVersion = extra["gson_version"]
val cliktVersion = extra["clikt_version"]

dependencies {
    antlr("org.antlr:antlr4:$antlrVersion")
    api(project(":core"))
    api(project(":cli"))
    api(project(":antlr"))
    api("org.eclipse.emf:org.eclipse.emf.common:2.23.0")
    api("org.eclipse.emf:org.eclipse.emf.ecore:2.25.0")
    api("org.eclipse.emf:org.eclipse.emf.ecore.xmi:2.16.0")
    api("org.eclipse.emfcloud:emfjson-jackson:2.0.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    implementation("com.google.code.gson:gson:$gsonVersion")

    api("com.github.ajalt.clikt:clikt:$cliktVersion")

    testImplementation("org.antlr:antlr4-runtime:$antlrVersion")
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

idea {
    module {
        testSourceDirs = testSourceDirs + file("generated-test-src/antlr/main")
    }
}

publishing {
    addSonatypeRepo(project)
    addPublication("kolasu_emf", "EMF integration for Kolasu", project)
}

signing {
    sign(publishing.publications["kolasu_emf"])
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
