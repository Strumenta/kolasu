import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    alias(libs.plugins.vanniktech.publish)
    id("antlr")
    id("idea")
    id("signing")
    id("org.jetbrains.dokka")
}

dependencies {
    implementation(project(":core"))
    testImplementation(kotlin("test-junit", libs.versions.kotlin.get()))
}

// Configure Kotlin compilation source sets
tasks.named<KotlinCompile>("compileKotlin").configure {
    source(sourceSets["main"].allJava, sourceSets["main"].kotlin)
}

tasks.named<KotlinCompile>("compileTestKotlin").configure {
    source(sourceSets["test"].kotlin)
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
    publishToMavenCentral(true)
    signAllPublications()
}
// Task dependencies
tasks.named("dokkaJavadoc") {
    dependsOn(":core:compileKotlin")
}
tasks.named("runKtlintCheckOverMainSourceSet") {
    dependsOn("generateGrammarSource")
}
tasks.named("runKtlintCheckOverTestSourceSet") {
    dependsOn("generateTestGrammarSource")
}
tasks.named("compileTestKotlin") {
    dependsOn("generateTestGrammarSource")
}
