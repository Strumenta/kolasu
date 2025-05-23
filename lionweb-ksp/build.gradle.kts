import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.Sign
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp") version libs.versions.ksp.get()
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka")
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.get())

    registerFeature("cli") {
        usingSourceSet(sourceSets["main"])
    }
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:${libs.versions.ksp.get()}")
    implementation(project(":core"))
}

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
        create<MavenPublication>("kolasu_lionweb_ksp") {
            from(components["java"])
            artifactId = "kolasu-${project.name}"
            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("javadocJar"))
            suppressPomMetadataWarningsFor("cliApiElements")
            suppressPomMetadataWarningsFor("cliRuntimeElements")

            pom {
                name.set("kolasu-${project.name}")
                description.set("Kolasu classes generation for LIonWeb")
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

signing {
    sign(publishing.publications["kolasu_lionweb_ksp"])
}

tasks.named("dokkaJavadoc") {
    dependsOn(":core:compileKotlin")
}