plugins {
    kotlin("jvm")
    alias(libs.plugins.vanniktech.publish)
    idea
    signing
    alias(libs.plugins.ktlint)
    id("org.jetbrains.dokka")
}

dependencies {
    api(project(":core"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    testImplementation(libs.kotlin.test)
}

mavenPublishing {
    coordinates(
        groupId = project.group.toString(),
        artifactId = "kolasu-" + project.name,
        version = project.version as String,
    )

    pom {
        name.set("kolasu-" + project.name)
        description.set("Framework to work with AST and building languages. Integrated with ANTLR.")
        version = project.version as String
        packaging = "jar"
        url.set("https://github.com/strumenta/kolasu")

        scm {
            connection.set("scm:git:https://github.com/strumenta/kolasu.git")
            developerConnection.set("scm:git:git@github.com:strumenta/kolasu.git")
            url.set("https://github.com/strumentao/kolasu.git")
        }

        licenses {
            license {
                name.set("Apache Licenve V2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
                distribution.set("repo")
            }
        }

        // The developers entry is strictly required by Maven Central
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
    publishToMavenCentral(true)
    signAllPublications()
}
