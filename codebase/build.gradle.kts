

plugins {
    kotlin("jvm")
    alias(libs.plugins.ktlint)
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka")
}

val jvmVersion = project.property("jvm_version") as String
val kotlinVersion = project.property("kotlin_version") as String

val isReleaseVersion = !(project.version as String).endsWith("-SNAPSHOT")

java {
    sourceCompatibility = JavaVersion.toVersion(jvmVersion)
    targetCompatibility = JavaVersion.toVersion(jvmVersion)
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    implementation(project(":core"))
    implementation(project(":lionweb"))
    implementation(libs.starlasu.specs)
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    implementation(libs.gson)
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
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
    dependsOn(":lionweb:jar")
}
