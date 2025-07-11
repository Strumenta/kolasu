import org.gradle.api.publish.maven.MavenPublication

plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    alias(libs.plugins.superPublish)
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
    implementation(kotlin("stdlib", libs.versions.kotlin.get()))
    implementation(kotlin("reflect", libs.versions.kotlin.get()))
    "cliImplementation"("com.github.ajalt.clikt:clikt:${libs.versions.clikt.get()}")
    testImplementation(kotlin("test-junit", libs.versions.kotlin.get()))
    testImplementation("com.google.code.gson:gson:${libs.versions.gson.get()}")
    implementation(libs.starlasu.specs)

    api(libs.lionweb.java)
    api(libs.lionweb.kotlin.core) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-test-junit5")
    }

    api(project(":core"))
    implementation(libs.starlasu.specs)
}

// Maven publishing
publishing {
    repositories {
        maven {
            val releaseRepo = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotRepo = uri("https://oss.sonatype.org/content/repositories/snapshots/")
            url = if (!project.version.toString().endsWith("SNAPSHOT")) releaseRepo else snapshotRepo

            credentials {
                username = project.findProperty("ossrhTokenUsername") as? String ?: "Unknown user"
                password = project.findProperty("ossrhTokenPassword") as? String ?: "Unknown password"
            }
        }
    }

    publications {
        create<MavenPublication>("kolasu_lionweb") {
            from(components["java"])
            artifactId = "kolasu-${project.name}"
            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("javadocJar"))
            suppressPomMetadataWarningsFor("cliApiElements")
            suppressPomMetadataWarningsFor("cliRuntimeElements")

            pom {
                name.set("kolasu-${project.name}")
                description.set("Integration of Kolasu with LIonWeb")
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
    sign(publishing.publications["kolasu_lionweb"])
}

// Ensure Dokka runs after compilation
tasks.named("dokkaJavadoc") {
    dependsOn(":core:compileKotlin")
}
