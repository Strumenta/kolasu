plugins {
    kotlin("multiplatform")
    id("org.jlleitschuh.gradle.ktlint")
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.kover")
}

val cliktVersion = extra["clikt_version"]
val gsonVersion = extra["gson_version"]
val antlrKotlinVersion = extra["antlrKotlinVersion"]

kotlin {
    jvm {
        withJava()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":ast"))
                implementation("com.strumenta:antlr-kotlin-runtime:$antlrKotlinVersion")
            }
        }
    }
}

publishing {
    addSonatypeRepo(project)
    // addPublication("kolasu_ast", "Framework to work with AST and building languages", project)
}

signing {
//    sign(publishing.publications["kolasu_ast"])
}
