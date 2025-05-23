import org.gradle.api.publish.maven.MavenPublication

plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    id("maven-publish")
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
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.test)
    implementation(libs.kotlin.test.junit5)
    implementation(libs.junit.jupiter)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlin.compiler.embeddable)

    implementation(libs.gson) {
        version {
            strictly(libs.versions.gson.get())
        }
    }

    implementation(libs.clikt)
    implementation(libs.clikt)

    implementation(libs.lionwebjava)
    implementation(libs.lionwebjavaemf)

    api(project(":core"))
    api(project(":lionweb"))
}

publishing {
    repositories {
        maven {
            val releaseRepo = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotRepo = uri("https://oss.sonatype.org/content/repositories/snapshots/")
            url = if (!version.toString().endsWith("SNAPSHOT")) releaseRepo else snapshotRepo

            credentials {
                username = project.findProperty("ossrhTokenUsername") as? String ?: "Unknown user"
                password = project.findProperty("ossrhTokenPassword") as? String ?: "Unknown password"
            }
        }
    }

    publications {
        create<MavenPublication>("kolasu_lionweb_gen") {
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
    sign(publishing.publications["kolasu_lionweb_gen"])
}

tasks.test {
    useJUnitPlatform()
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.google.code.gson") {
            useVersion(libs.versions.gson.get())
        }
    }
}

tasks.named("dokkaJavadoc") {
    dependsOn(":core:compileKotlin", ":emf:compileKotlin", ":lionweb:jar")
}
