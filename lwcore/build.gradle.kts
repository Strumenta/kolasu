//import com.vanniktech.maven.publish.SonatypeHost
import java.net.URI

plugins {
    `jvm-test-suite`
    id("org.jetbrains.kotlin.jvm")
    //alias(libs.plugins.kotlinJvm)
    //alias(libs.plugins.dokka)
    alias(libs.plugins.ktlint)
    id("java-library")
    alias(libs.plugins.superPublish)
}

repositories {
    mavenLocal()
    mavenCentral()
}

tasks.withType<Test>().all {
    testLogging {
        showStandardStreams = true
        showExceptions = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

dependencies {
    api(libs.lionwebjava)
    api(libs.lionwebkotlincore)
//    api(libs.lionwebkotlincore) {
//        exclude group: "org.jetbrains.kotlin", module: "kotlin-test-junit5"
//    }
}

val jvmVersion = extra["jvm_version"] as String
val normalizedJvmVersion = if (jvmVersion.startsWith("1.")) jvmVersion.removePrefix("1.") else jvmVersion

java {
    sourceCompatibility = JavaVersion.toVersion(jvmVersion)
    targetCompatibility = JavaVersion.toVersion(jvmVersion)
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
    kotlinOptions {
        jvmTarget = jvmVersion
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(normalizedJvmVersion.toInt()))
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(normalizedJvmVersion.toInt()))
    }
}


tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<Test> {
    testLogging {
        events("standardOut", "passed", "skipped", "failed")
    }
}

val isReleaseVersion = (version as String).endsWith("SNAPSHOT")

publishing {

    repositories {
        maven {
            val releaseRepo = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotRepo = "https://oss.sonatype.org/content/repositories/snapshots/"
            url = URI(if (isReleaseVersion)  releaseRepo else snapshotRepo)
            credentials {
                username = if (project.hasProperty("ossrhTokenUsername")) project.properties["ossrhTokenUsername"] as String else "Unknown user"
                password = if (project.hasProperty("ossrhTokenPassword")) project.properties["ossrhTokenPassword"] as String  else "Unknown password"
            }
        }
    }

    publishing {
        publications {
            create<MavenPublication>("kolasu_lwcore") {
                from(components["java"])
                artifactId = "kolasu-${project.name}"
                //artifact(sourcesJar)
                //artifact(javadocJar)
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
}
