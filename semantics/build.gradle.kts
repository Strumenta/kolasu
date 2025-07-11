plugins {
    kotlin("jvm")
    alias(libs.plugins.superPublish)
    idea
    signing
    alias(libs.plugins.ktlint)
    id("org.jetbrains.dokka")
}

val jvmVersion: String = libs.versions.jvm.get()
val isReleaseVersion: Boolean = !(project.version as String).endsWith("SNAPSHOT")

java {
    setSourceCompatibility(jvmVersion)
    setTargetCompatibility(jvmVersion)
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    api(project(":core"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    testImplementation(libs.kotlin.test)
}

publishing {
    repositories {
        maven {
            val releaseRepo = uri("https://central.sonatype.com/publish/repositories/releases/")
            val snapshotRepo = uri("https://central.sonatype.com/repository/maven-snapshots/")
            url = if (!version.toString().endsWith("SNAPSHOT")) releaseRepo else snapshotRepo

            credentials {
                username = project.findProperty("mavenCentralUsername") as? String ?: "Unknown user"
                password = project.findProperty("mavenCentralPassword") as? String ?: "Unknown password"
            }
        }
    }
    publications {
        create<MavenPublication>("kolasu_semantics") {
            artifactId = "kolasu-" + project.name
            from(components["java"])
            pom {
                name = "kolasu-" + project.name
                description = "Semantic enrichment facilities for Kolasu ASTs."
                version = project.version as? String
                packaging = "jar"
                url = "https://github.com/Strumenta/kolasu"
                scm {
                    connection = "scm:git:https://github.com/Strumenta/kolasu.git"
                    developerConnection = "scm:git:git@github.com:Strumenta/kolasu.git"
                    url = "https://github.com/Strumenta/kolasu.git"
                }
                licenses {
                    license {
                        name = "Apache Licenve V2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                        distribution = "repo"
                    }
                }
                developers {
                    developer {
                        id = "loradd"
                        name = "Lorenzo Addazi"
                        email = "lorenzo.addazi@strumenta.com"
                    }
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["kolasu_semantics"])
}

tasks.findByName("dokkaJavadoc")?.dependsOn(":core:compileKotlin")
