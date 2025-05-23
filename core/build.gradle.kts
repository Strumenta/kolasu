plugins {
    kotlin("jvm")
    alias(libs.plugins.ktlint)
    id("maven-publish")
    id("antlr")
    id("idea")
    id("signing")
    id("org.jetbrains.dokka")
}

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())

    registerFeature("cli") {
        usingSourceSet(sourceSets["main"])
    }
}

dependencies {
    antlr(libs.antlr)
    implementation(libs.antlrRuntime)
    implementation(kotlin("stdlib", libs.versions.kotlin.get()))
    implementation(kotlin("reflect", libs.versions.kotlin.get()))
    implementation(kotlin("test", libs.versions.kotlin.get()))
    implementation(libs.gson)
    api(libs.clikt)
    testImplementation(kotlin("test-junit", libs.versions.kotlin.get()))
    api(libs.lionwebjava)
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

// Maven Publishing
publishing {
    repositories {
        maven {
            val releaseRepo = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotRepo = uri("https://oss.sonatype.org/content/repositories/snapshots/")
            url = if (!version.toString().endsWith("SNAPSHOT")) releaseRepo else snapshotRepo

            credentials {
                username = project.findProperty("ossrhTokenUsername") as? String ?: "Unknown user"
                password = project.findProperty("ossrhTokenPassword") as? String ?: "Unknown password"
            }
        }
    }

    publications {
        create<MavenPublication>("kolasu_core") {
            from(components["java"])
            artifactId = "kolasu-${project.name}"
            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("javadocJar"))
            suppressPomMetadataWarningsFor("cliApiElements")
            suppressPomMetadataWarningsFor("cliRuntimeElements")
            pom {
                name.set("kolasu-${project.name}")
                description.set("Framework to work with AST and building languages. Integrated with ANTLR.")
                version = project.version.toString()
                packaging = "jar"
                url.set("https://github.com/Strumenta/kolasu")
                scm {
                    connection.set("scm:git:https://github.com/Strumenta/kolasu.git")
                    developerConnection.set("scm:git:git@github.com:Strumenta/kolasu.git")
                    url.set("https://github.com/Strumenta/kolasu.git")
                }
                licenses {
                    license {
                        name.set("Apache License V2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("ftomassetti")
                        name.set("Federico Tomassetti")
                        email.set("federico@strumenta.com")
                    }
                    developer {
                        id.set("alessiostalla")
                        name.set("Alessio Stalla")
                        email.set("alessio.stalla@strumenta.com")
                    }
                    developer {
                        id.set("lorenzoaddazi")
                        name.set("Lorenzo Addazi")
                        email.set("lorenzo.addazi@strumenta.com")
                    }
                }
            }
        }
    }
}

// Signing
signing {
    sign(publishing.publications["kolasu_core"])
}
