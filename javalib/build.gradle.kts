dependencies {
    implementation(project(":core"))
}

publishing {
    addSonatypeRepo(project)
    addPublication("kolasu_javalib", "Java integration for Kolasu", project)
}

signing {
    sign(publishing.publications["kolasu_javalib"])
}
