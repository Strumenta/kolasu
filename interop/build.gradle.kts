plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.ktlint)
    id("maven-publish")
    id("antlr")
    id("idea")
    id("signing")
    id("org.jetbrains.dokka")
}

java {
    sourceCompatibility = JavaVersion.toVersion(project.extra["jvm_version"] as String)
    targetCompatibility = JavaVersion.toVersion(project.extra["jvm_version"] as String)
}

dependencies {
    api(libs.lionwebjava)
    implementation("org.jetbrains.kotlin:kotlin-test:${project.extra["kotlin_version"]}")
}

val interopVersion = extra["interop_version"] as String
val isReleaseVersion = !interopVersion.endsWith("-SNAPSHOT")

publishing {
    repositories {
        maven {
            val releaseRepo = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotRepo = uri("https://oss.sonatype.org/content/repositories/snapshots/")
            url = if (isReleaseVersion) releaseRepo else snapshotRepo
            credentials {
                username = if (project.hasProperty("ossrhTokenUsername")) {
                    project.property("ossrhTokenUsername").toString()
                } else {
                    "Unknown user"
                }
                password = if (project.hasProperty("ossrhTokenPassword")) {
                    project.property("ossrhTokenPassword").toString()
                } else {
                    "Unknown password"
                }
            }
        }
    }

    publications {
        create<MavenPublication>("kolasu_interop") {
            from(components["java"])
            artifactId = "kolasu-${project.name}"
            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("javadocJar"))

            pom {
                name.set("kolasu-${project.name}")
                description.set("Framework to work with AST and building languages. Integrated with ANTLR.")
                version = interopVersion
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

signing {
    sign(publishing.publications["kolasu_interop"])
}

tasks.findByName("generateGrammarSource")?.dependsOn("sourcesJar")
tasks.findByName("runKtlintCheckOverTestSourceSet")?.dependsOn("generateTestGrammarSource")
tasks.findByName("runKtlintFormatOverTestSourceSet")?.dependsOn("generateTestGrammarSource")
