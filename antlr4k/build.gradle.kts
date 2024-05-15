plugins {
    kotlin("multiplatform")
    alias(libs.plugins.ktlint)
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.kover")
}

val antlrKotlinVersion = extra["antlrKotlinVersion"]
val kotlinVersion = extra["kotlinVersion"]

kotlin {
    jvm {
        withJava()
    }
    js {
        browser()
        nodejs()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":ast"))
                implementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
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
