plugins {
    kotlin("multiplatform")
    alias(libs.plugins.ktlint)
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka")
    alias(libs.plugins.kover)
}

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
}

publishing {
    addSonatypeRepo(project)
    // addPublication("kolasu_ast", "Framework to work with AST and building languages", project)
}

signing {
//    sign(publishing.publications["kolasu_ast"])
}
