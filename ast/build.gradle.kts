import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.JavadocJar

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.ktlint)
    //id("maven-publish")
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

signing {
//    sign(publishing.publications["kolasu_ast"])
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}
