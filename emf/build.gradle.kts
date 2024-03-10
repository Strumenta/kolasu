plugins {
    id("antlr")
}

val kotlinVersion = extra["kotlinVersion"]

dependencies {
    antlr(libs.antlr4Tool)
    api(project(":core"))
    api(project(":cli"))
    api(project(":antlr4j"))
    api(project(":ast"))
    api("org.eclipse.emf:org.eclipse.emf.common:2.23.0")
    api("org.eclipse.emf:org.eclipse.emf.ecore:2.25.0")
    api("org.eclipse.emf:org.eclipse.emf.ecore.xmi:2.16.0")
    api("org.eclipse.emfcloud:emfjson-jackson:2.0.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.1")
    implementation(libs.gson)

    api(libs.clikt)

    testImplementation(libs.antlr4jRuntime)
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
