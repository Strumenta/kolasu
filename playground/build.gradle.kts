val gsonVersion = extra["gson_version"]

dependencies {
    api(project(":core"))
    api(project(":emf"))
    implementation("com.google.code.gson:gson:$gsonVersion")
}

publishing {
    addSonatypeRepo(project)
    addPublication("kolasu_playground", "Strumenta's Playground integration for Kolasu", project)
}

signing {
    sign(publishing.publications["kolasu_playground"])
}
