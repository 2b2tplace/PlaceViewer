rootProject.name = "placeviewer-build-logic"

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }

    versionCatalogs {
        register("libs") {
            from(files("../gradle/libs.versions.toml")) // include from parent project
        }
    }
}
