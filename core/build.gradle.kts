plugins {
    kotlin("jvm")
    alias(libs.plugins.ktlint)
    alias(libs.plugins.vanniktech.publish)
    id("antlr")
    id("idea")
    id("signing")
    id("org.jetbrains.dokka")
    id("com.strumenta.starlasu.build.plugin")
}

dependencies {
    antlr(libs.antlr)
    implementation(libs.antlr.runtime)
    implementation(libs.starlasu.specs)
    implementation(kotlin("stdlib", libs.versions.kotlin.get()))
    implementation(kotlin("reflect", libs.versions.kotlin.get()))
    implementation(libs.gson)
    api(libs.clikt)
    api(libs.lionweb.java)

    implementation(kotlin("test", libs.versions.kotlin.get()))
    testImplementation(kotlin("test-junit", libs.versions.kotlin.get()))
}

tasks.named<AntlrTask>("generateTestGrammarSource") {
    maxHeapSize = "64m"
    arguments.addAll(listOf("-package", "com.strumenta.simplelang"))
    outputDirectory = file("generated-test-src/antlr/main/com/strumenta/simplelang")
}

tasks.named("runKtlintCheckOverMainSourceSet") {
    dependsOn("generateGrammarSource")
}

tasks.named("compileKotlin") {
    dependsOn("generateGrammarSource")
}
tasks.named("compileJava") {
    dependsOn("generateGrammarSource", "generateTestGrammarSource")
}
tasks.named("compileTestKotlin") {
    dependsOn("generateTestGrammarSource")
}
tasks.named("runKtlintCheckOverTestSourceSet") {
    dependsOn("generateTestGrammarSource")
}
tasks.named("runKtlintFormatOverTestSourceSet") {
    dependsOn("generateTestGrammarSource")
}
tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileKotlin").configure {
    source(sourceSets["main"].allJava, sourceSets["main"].kotlin)
}
tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileTestKotlin").configure {
    source(sourceSets["test"].kotlin)
}

sourceSets.named("test") {
    java.srcDir("generated-test-src/antlr/main")
}

tasks.named<Delete>("clean") {
    delete("generated-src", "generated-test-src")
}

idea {
    module {
        testSources.from(file("generated-test-src/antlr/main"))
    }
}

// Some tasks are created during the configuration, and therefore we need to set the dependencies involving
// them after the configuration has been completed
project.afterEvaluate {
    tasks.named("dokkaJavadocJar") {
        dependsOn(tasks.named("dokkaJavadoc"))
    }
    tasks.named("signMavenPublication") {
        dependsOn(tasks.named("dokkaJavadocJar"))
        dependsOn(tasks.named("javadocJar"))
        dependsOn(tasks.named("sourcesJar"))
    }
    tasks.named("sourcesJar") {
        dependsOn("generateGrammarSource")
    }
}
