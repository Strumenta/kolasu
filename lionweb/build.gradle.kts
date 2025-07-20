import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    alias(libs.plugins.vanniktech.publish)
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
    implementation(kotlin("stdlib", libs.versions.kotlin.get()))
    implementation(kotlin("reflect", libs.versions.kotlin.get()))
    "cliImplementation"("com.github.ajalt.clikt:clikt:${libs.versions.clikt.get()}")
    testImplementation(kotlin("test-junit", libs.versions.kotlin.get()))
    testImplementation("com.google.code.gson:gson:${libs.versions.gson.get()}")
    implementation(libs.starlasu.specs)

    api(libs.lionweb.java)
    api(libs.lionweb.kotlin)

    api(project(":core"))
    implementation(libs.starlasu.specs)
}
mavenPublishing {
    coordinates(
        groupId = project.group.toString(),
        artifactId = "kolasu-" + project.name,
        version = project.version as String,
    )

    pom {
        name.set("kolasu-" + project.name)
        description.set("Framework to work with AST and building languages. Integrated with ANTLR.")
        version = project.version as String
        packaging = "jar"
        url.set("https://github.com/strumenta/kolasu")

        scm {
            connection.set("scm:git:https://github.com/strumenta/kolasu.git")
            developerConnection.set("scm:git:git@github.com:strumenta/kolasu.git")
            url.set("https://github.com/strumentao/kolasu.git")
        }

        licenses {
            license {
                name.set("Apache Licenve V2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
                distribution.set("repo")
            }
        }

        // The developers entry is strictly required by Maven Central
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
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, true)
    signAllPublications()
}

// Ensure Dokka runs after compilation
tasks.named("dokkaJavadoc") {
    dependsOn(":core:compileKotlin")
}
