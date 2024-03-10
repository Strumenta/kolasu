dependencies {
    api(project(":core"))
    api(project(":emf"))
    implementation(libs.gson)
}

publishing {
    addSonatypeRepo(project)
    addPublication("kolasu_playground", "Strumenta's Playground integration for Kolasu", project)
}

signing {
    sign(publishing.publications["kolasu_playground"])
}
