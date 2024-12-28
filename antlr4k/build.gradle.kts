plugins {
    kotlin("multiplatform")
    alias(libs.plugins.ktlint)
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka")
    alias(libs.plugins.kover)
    id("com.vanniktech.maven.publish") version "0.28.0"
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


mavenPublishing {
    coordinates(project.group as String, "kolasu-"+project.name, project.version as String)

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