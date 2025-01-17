import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.ktlint)
    // id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish") version "0.28.0"
    alias(libs.plugins.kover)
}

kotlin {
    jvm {
        withJava()
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
    js {
        browser()
        nodejs()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api("com.badoo.reaktive:reaktive:2.0.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("reflect"))
            }
        }
        val jsMain by getting
        val jvmMain by getting
    }
    jvm().compilations.all {
        compilerOptions.configure {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
}

publishing {
    addSonatypeRepo(project)
//    addPublication("kolasu_ast",
//        "Framework to work with AST and building languages", project,
//        kotlinMultiplatform = true)
}

mavenPublishing {
    configure(
        KotlinMultiplatform(
            // configures the -javadoc artifact, possible values:
            // - `JavadocJar.None()` don't publish this artifact
            // - `JavadocJar.Empty()` publish an emprt jar
            // - `JavadocJar.Dokka("dokkaHtml")` when using Kotlin with Dokka, where `dokkaHtml` is the name of the Dokka task that should be used as input
            javadocJar = JavadocJar.Dokka("dokkaHtml"),
            // whether to publish a sources jar
            sourcesJar = true,
            // configure which Android library variants to publish if this project has an Android target
            // defaults to "release" when using the main plugin and nothing for the base plugin
            // androidVariantsToPublish = listOf("debug", "release"),
        ),
    )
    coordinates(project.group as String, "kolasu-ast", project.version as String)

    pom {
        name.set("kolasu-" + project.name)
        description.set("Framework to work with AST and building languages")
        version = project.version as String
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
        }
    }
}

// signing {
// //    sign(publishing.publications["kolasu_ast"])
// }

signing {
    sign(publishing.publications)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks {
    named("publishJsPublicationToMavenLocal") {
        dependsOn("signJvmPublication")
        dependsOn("signKotlinMultiplatformPublication")
    }
    named("publishJvmPublicationToMavenLocal") {
        dependsOn("signJsPublication")
        dependsOn("signKotlinMultiplatformPublication")
    }
    named("publishKotlinMultiplatformPublicationToMavenLocal") {
        dependsOn("signJsPublication")
        dependsOn("signJvmPublication")
    }
}
