

plugins {
    kotlin("jvm")
    alias(libs.plugins.ktlint)
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka")
}

val jvm_version = project.property("jvm_version") as String
val kotlin_version = project.property("kotlin_version") as String
val gson_version = project.property("gson_version") as String

val isReleaseVersion = !(project.version as String).endsWith("-SNAPSHOT")

java {
    sourceCompatibility = JavaVersion.toVersion(jvm_version)
    targetCompatibility = JavaVersion.toVersion(jvm_version)
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    implementation(project(":core"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")
    implementation("com.google.code.gson:gson:$gson_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}

publishing {
    repositories {
        maven {
            val releaseRepo = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotRepo = uri("https://oss.sonatype.org/content/repositories/snapshots/")
            url = if (isReleaseVersion) releaseRepo else snapshotRepo
            credentials {
                username = project.findProperty("ossrhTokenUsername") as String? ?: "Unknown user"
                password = project.findProperty("ossrhTokenPassword") as String? ?: "Unknown password"
            }
        }
    }
    publications {
        create<MavenPublication>("kolasu_codebase") {
            from(components["java"])
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

signing {
    sign(publishing.publications["kolasu_codebase"])
}

tasks.named("dokkaJavadoc").configure {
    dependsOn(":core:compileKotlin")
}
