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

//java {
//    sourceCompatibility = "$jvm_version"
//    targetCompatibility = "$jvm_version"
//}

val antlr_version = extra["antlr_version"]
val kotlin_version = extra["kotlin_version"]

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

generateTestGrammarSource {
    maxHeapSize = "64m"
    arguments += ["-package", "com.strumenta.simplelang"]
    outputDirectory = new File("generated-test-src/antlr/main/com/strumenta/simplelang".toString())
}

sourceSets.getByName("test") {
    java.srcDir("src/test/java")
    java.srcDir("generated-test-src/antlr/main")
}

clean {
    delete("generated-src")
    delete("generated-test-src")
}

idea {
    module {
        testSourceDirs += file("generated-test-src/antlr/main")
    }
}

// TODO remove
ext.isReleaseVersion = !kolasu_version.endsWith("SNAPSHOT")

publishing {

    repositories {
        maven {
            val releaseRepo = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotRepo = "https://oss.sonatype.org/content/repositories/snapshots/"
            url = URI(if (isReleaseVersion) releaseRepo else snapshotRepo)
            credentials {
                username = if (project.hasProperty("ossrhUsername")) ossrhUsername else "Unknown user"
                password = if (project.hasProperty("ossrhPassword")) ossrhPassword else "Unknown password"
            }
        }
    }

    publications {
        kolasu_antlr(MavenPublication) {
            from(components.java)
            artifactId = "kolasu-" + project.name
            artifact("sourcesJar")
            artifact("javadocJar")
            suppressPomMetadataWarningsFor("cliApiElements")
            suppressPomMetadataWarningsFor("cliRuntimeElements")
            pom {
                name = "kolasu-" + project.name
                description = "Framework to work with AST and building languages. Integrated with ANTLR."
                version = project.version
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
                        id = "ftomassetti"
                        name = "Federico Tomassetti"
                        email = "federico@strumenta.com"
                    }
                    developer {
                        id = "alessiostalla"
                        name = "Alessio Stalla"
                        email = "alessio.stalla@strumenta.com"
                    }
                    developer {
                        id = "lorenzoaddazi"
                        name = "Lorenzo Addazi"
                        email = "lorenzo.addazi@strumenta.com"
                    }
                }

            }
        }
    }
}

signing {
    sign(publishing.publications.kolasu_antlr)
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
}