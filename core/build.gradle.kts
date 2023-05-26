import org.jetbrains.dokka.gradle.DokkaTask

val clikt_version = extra["clikt_version"]

dependencies {
    implementation("com.github.salomonbrys.kotson:kotson:2.5.0")
    implementation("org.redundent:kotlin-xml-builder:1.9.0")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.8.0")

    implementation("com.github.ajalt.clikt:clikt:$clikt_version")
}

//tasks.register<Jar>("sourcesJar") {
//    archiveBaseName.set(project.name)
//    archiveClassifier.set("sources")
//    // See https://discuss.gradle.org/t/why-subproject-sourceset-dirs-project-sourceset-dirs/7376/5
//    // Without the closure, parent sources are used for children too
//    from(sourceSets["main"].allSource)
//}

//tasks.register<Jar>("kdocJar") {
//    dependsOn("dokkaJavadoc")
//    from((tasks.named("dokkaJavadoc").get() as DokkaTask).outputDirectory)
//    archiveClassifier.set("javadoc")
//}

publishing {
    addSonatypeRepo(project)
    addPublication("kolasu_core", "Framework to work with AST and building languages", project)
}

signing {
    sign(publishing.publications["kolasu_core"])
}
