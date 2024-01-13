val kotlinVersion = extra["kotlinVersion"]

dependencies {
    api("com.badoo.reaktive:reaktive:2.0.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
}

publishing {
    addSonatypeRepo(project)
    addPublication("kolasu_core", "Framework to work with AST and building languages", project)
}

signing {
    sign(publishing.publications["kolasu_core"])
}
