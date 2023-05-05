import java.net.URI

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jlleitschuh.gradle.ktlint")
    id("maven-publish")
    id("antlr")
    id("idea")
    id("signing")
    id("org.jetbrains.dokka")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val antlr_version = extra["antlr_version"]
val kotlin_version = extra["kotlin_version"]
// val version = extra["kolasu_version"] as String
val isReleaseVersion = !(version as String).endsWith("SNAPSHOT")

dependencies {
    antlr("org.antlr:antlr4:$antlr_version")
    implementation("org.antlr:antlr4-runtime:$antlr_version")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    // we define some testing utilities
    implementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")
    implementation(project(":core"))

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}

tasks.generateTestGrammarSource {
    maxHeapSize = "64m"
    arguments = arguments + listOf("-package", "com.strumenta.simplelang")
    outputDirectory = File("generated-test-src/antlr/main/com/strumenta/simplelang".toString())
}

sourceSets.getByName("test") {
    java.srcDir("src/test/java")
    java.srcDir("generated-test-src/antlr/main")
}

tasks.clean {
    delete("generated-src")
    delete("generated-test-src")
}

idea {
    module {
        testSourceDirs = testSourceDirs + file("generated-test-src/antlr/main")
    }
}

publishing {

    repositories {
        maven {
            val releaseRepo = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotRepo = "https://oss.sonatype.org/content/repositories/snapshots/"
            url = URI(if (isReleaseVersion) releaseRepo else snapshotRepo)
            credentials {
                username = if (project.hasProperty("ossrhUsername")) {
                    project.properties["ossrhUsername"] as String
                } else {
                    "Unknown user"
                }
                password = if (project.hasProperty("ossrhPassword")) {
                    project.properties["ossrhPassword"] as String
                } else {
                    "Unknown password"
                }
            }
        }
    }

    publications {
        create<MavenPublication>("kolasu_antlr") {
            from(components["java"])
            artifactId = "kolasu-" + project.name
            artifact("sourcesJar")
            artifact("javadocJar")
            suppressPomMetadataWarningsFor("cliApiElements")
            suppressPomMetadataWarningsFor("cliRuntimeElements")
            pom {
                name.set("kolasu-" + project.name)
                description.set("Framework to work with AST and building languages. Integrated with ANTLR.")
                version = project.version as String
                packaging = "jar"
                url.set("https://github.com/Strumenta/kolasu")

                scm {
                    connection.set("scm:git:https://github.com/Strumenta/kolasu.git")
                    developerConnection.set("scm:git:git@github.com:Strumenta/kolasu.git")
                    url.set("https://github.com/Strumenta/kolasu.git")
                }

                licenses {
                    license {
                        name.set("Apache Licenve V2.0")
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
    sign(publishing.publications["kolasu_antlr"])
}

tasks {
    named("compileTestKotlin") {
        dependsOn("generateTestGrammarSource")
    }
    named("compileKotlin") {
        dependsOn("generateGrammarSource")
    }
    named("compileJava") {
        dependsOn("generateGrammarSource")
        dependsOn("generateTestGrammarSource")
    }
    named("compileTestKotlin") {
        dependsOn("generateTestGrammarSource")
    }
    named("runKtlintCheckOverTestSourceSet") {
        dependsOn("generateTestGrammarSource")
    }
}
