dependencies {
    implementation(project(":core"))
    implementation(project(":ast"))
}

publishing {
    addSonatypeRepo(project)
    addPublication("kolasu_javalib", "Java integration for Kolasu", project)
}

signing {
    sign(publishing.publications["kolasu_javalib"])
}
