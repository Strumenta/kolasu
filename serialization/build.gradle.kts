val clikt_version = extra["clikt_version"]
val gson_version = extra["gson_version"]

dependencies {
    implementation("org.redundent:kotlin-xml-builder:1.9.0")
    implementation("com.google.code.gson:gson:$gson_version")

    implementation(project(":core"))
}

publishing {
    addSonatypeRepo(project)
    addPublication("kolasu_serialization", "Framework to work with AST and building languages", project)
}

signing {
    sign(publishing.publications["kolasu_serialization"])
}
