val kotlinVersion = extra["kotlinVersion"]

dependencies {
    implementation("org.redundent:kotlin-xml-builder:1.9.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation(libs.gson)

    implementation(project(":core"))
    implementation(project(":ast"))
}

publishing {
    addSonatypeRepo(project)
    addPublication("kolasu_serialization", "Framework to work with AST and building languages", project)
}

signing {
    sign(publishing.publications["kolasu_serialization"])
}
