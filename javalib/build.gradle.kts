import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    alias(libs.plugins.vanniktech.publish)
    id("antlr")
    id("idea")
    id("signing")
    id("org.jetbrains.dokka")
}

dependencies {
    implementation(project(":core"))
    testImplementation(kotlin("test-junit", libs.versions.kotlin.get()))
}

// Configure Kotlin compilation source sets
tasks.named<KotlinCompile>("compileKotlin").configure {
    source(sourceSets["main"].allJava, sourceSets["main"].kotlin)
}

tasks.named<KotlinCompile>("compileTestKotlin").configure {
    source(sourceSets["test"].kotlin)
}

// Task dependencies
tasks.named("dokkaJavadoc") {
    dependsOn(":core:compileKotlin")
}
tasks.named("runKtlintCheckOverMainSourceSet") {
    dependsOn("generateGrammarSource")
}
tasks.named("runKtlintCheckOverTestSourceSet") {
    dependsOn("generateTestGrammarSource")
}
tasks.named("compileTestKotlin") {
    dependsOn("generateTestGrammarSource")
}

// Some tasks are created during the configuration, and therefore we need to set the dependencies involving
// them after the configuration has been completed
project.afterEvaluate {
    tasks.named("sourcesJar") {
        dependsOn("generateGrammarSource")
    }
}
