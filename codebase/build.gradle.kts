import com.vanniktech.maven.publish.SonatypeHost


plugins {
    kotlin("jvm")
    alias(libs.plugins.ktlint)
    id("signing")
    id("org.jetbrains.dokka")
    alias(libs.plugins.vanniktech.publish)
}

val jvmVersion = project.property("jvm_version") as String
val kotlinVersion = project.property("kotlin_version") as String

val isReleaseVersion = !(project.version as String).endsWith("-SNAPSHOT")

java {
    sourceCompatibility = JavaVersion.toVersion(jvmVersion)
    targetCompatibility = JavaVersion.toVersion(jvmVersion)
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

mavenPublishing {
    coordinates(
        groupId = project.group as String,
        artifactId = "kolasu-${project.name}",
        version = project.version as String,
    )

    pom {
        name.set("kolasu-" + project.name)
        description.set("JFramework to work with AST and building languages. Integrated with ANTLR.")
        version = project.version as String
        packaging = "jar"
        url.set("https://github.com/Strumenta/kolasu")

        scm {
            connection.set("scm:git:https://github.com/strumenta/kolasu.git")
            developerConnection.set("scm:git:git@github.com:strumenta/kolasu.git")
            url.set("https://github.com/strumenta/kolasu.git")
        }

        licenses {
            license {
                name.set("Apache License V2.0")
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

tasks.named("dokkaJavadoc").configure {
    dependsOn(":core:compileKotlin")
    dependsOn(":lionweb:jar")
}

// Some tasks are created during the configuration, and therefore we need to set the dependencies involving
// them after the configuration has been completed
project.afterEvaluate {
//    tasks.named("dokkaJavadocJar") {
//        dependsOn(tasks.named("dokkaJavadoc"))
//    }
    tasks.named("generateMetadataFileForMavenPublication") {
        //dependsOn(tasks.named("dokkaJavadocJar"))
        dependsOn(tasks.named("javadocJar"))
        dependsOn(tasks.named("sourcesJar"))
    }
    tasks.named("dokkaHtml") {
        dependsOn(tasks.named("compileKotlin"))
    }
}
