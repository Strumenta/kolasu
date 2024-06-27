import java.net.URI

plugins {
    kotlin("jvm")
    `maven-publish`
    idea
    signing
    alias(libs.plugins.ktlint)
    id("org.jetbrains.dokka")
}

val jvmVersion: String = properties["jvm_version"] as String
val kotlinVersion: String = properties["kotlinVersion"] as String
val isReleaseVersion: Boolean = !(project.version as String).endsWith("SNAPSHOT")

java {
    setSourceCompatibility(jvmVersion)
    setTargetCompatibility(jvmVersion)
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    api(project(":ast"))
    api(project(":core"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
}

publishing {
    repositories {
        maven {
            val releaseRepo = URI("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotRepo = URI("https://oss.sonatype.org/content/repositories/snapshots/")
            url = releaseRepo.takeIf { isReleaseVersion } ?: snapshotRepo
            credentials {
                username = project.property("ossrhTokenUsername") as String? ?: "Unknown user"
                password = project.property("ossrhTokenPassword") as String? ?: "Unknown password"
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

tasks {
    named("generateMetadataFileForKolasu_semanticsPublication") {
        dependsOn("kdocJar")
    }
}
