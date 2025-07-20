plugins {
    kotlin("jvm")
    alias(libs.plugins.ktlint)
    id("signing")
    id("org.jetbrains.dokka")
    alias(libs.plugins.vanniktech.publish)
}

val isReleaseVersion = !(project.version as String).endsWith("-SNAPSHOT")

dependencies {
    implementation(project(":core"))
    implementation(project(":lionweb"))
    implementation(kotlin("stdlib", libs.versions.kotlin.get()))
    implementation(kotlin("reflect", libs.versions.kotlin.get()))
    implementation(libs.starlasu.specs)
    implementation(libs.gson)

    testImplementation(kotlin("test", libs.versions.kotlin.get()))
    testImplementation(kotlin("test-junit", libs.versions.kotlin.get()))
}

publishing {
    repositories {
        maven {
            val releaseRepo = uri("https://central.sonatype.com/publish/repositories/releases/")
            val snapshotRepo = uri("https://central.sonatype.com/repository/maven-snapshots/")
            url = if (isReleaseVersion) releaseRepo else snapshotRepo
            credentials {
                username = project.findProperty("mavenCentralUsername") as? String ?: "Unknown user"
                password = project.findProperty("mavenCentralPassword") as? String ?: "Unknown password"
            }
        }
    }
    publications.withType<MavenPublication>().configureEach {
        if (name == "maven") {
            artifactId = "kolasu-${project.name}"
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
                }
            }
        }
    }
}

tasks.named("dokkaJavadoc").configure {
    dependsOn(":core:compileKotlin")
    dependsOn(":lionweb:jar")
}
