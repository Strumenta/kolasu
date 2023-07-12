val clikt_version = extra["clikt_version"]
val gson_version = extra["gson_version"]

dependencies {
    implementation("org.redundent:kotlin-xml-builder:1.9.0")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.8.0")
    implementation("com.google.code.gson:gson:$gson_version")

    api("com.github.ajalt.clikt:clikt:$clikt_version")
    api("io.reactivex.rxjava3:rxjava:3.1.1")
}

publishing {
    addSonatypeRepo(project)
    addPublication("kolasu_core", "Framework to work with AST and building languages", project)
}

signing {
    sign(publishing.publications["kolasu_core"])
}
