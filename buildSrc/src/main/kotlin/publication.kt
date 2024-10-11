import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import java.net.URI

fun Project.isReleaseVersion(): Boolean {
    return !((this.version as String).endsWith("-SNAPSHOT"))
}

fun PublishingExtension.addSonatypeRepo(project: Project) {
    repositories {
        maven {
            val releaseRepo = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotRepo = "https://oss.sonatype.org/content/repositories/snapshots/"
            url = URI(if (project.isReleaseVersion()) releaseRepo else snapshotRepo)
            credentials {
                username = if (project.hasProperty("ossrhTokenUsername")) {
                    project.properties["ossrhTokenUsername"] as String
                } else {
                    "Unknown user"
                }
                password = if (project.hasProperty("ossrhTokenPassword")) {
                    project.properties["ossrhTokenPassword"] as String
                } else {
                    "Unknown password"
                }
            }
        }
    }
}

fun PublishingExtension.addPublication(
    pubName: String,
    pubDescription: String,
    project: Project,
    kotlinMultiplatform: Boolean = false,
    addPrefix: Boolean = true
) {
    publications {
        if (kotlinMultiplatform) {
            TODO()
        } else {
            create<MavenPublication>(pubName) {
                from(project.components["java"])
                artifactId = if (addPrefix) "kolasu-" + project.name else project.name
                artifact(project.tasks.named("sourcesJar"))
                artifact(project.tasks.named("kdocJar"))
                suppressPomMetadataWarningsFor("cliApiElements")
                suppressPomMetadataWarningsFor("cliRuntimeElements")
                pom {
                    name.set(if (addPrefix) "kolasu-" + project.name else project.name)
                    description.set(pubDescription)
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
