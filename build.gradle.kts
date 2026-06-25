plugins {
    id("mod.base-conventions")
    id("me.glicz.access-widen") version "3.0.0"
}

val accessWidening by configurations.creating

dependencies {
    paperweight.paperDevBundle(libs.versions.paper)
    compileOnly(accessWiden(accessWidening))

    compileOnly(libs.ignite)
    compileOnly(libs.mixin)
    compileOnly(libs.mixinExtras)
    compileOnly("org.jetbrains:annotations:26.1.0")

    implementation("org.reflections:reflections:0.10.2")
    implementation("me.xuender:unidecode:0.0.7")

    annotationProcessor(libs.mixinExtras)
}

paperweight {
    addServerDependencyTo = setOf(accessWidening)
}

accessWiden {
    accessWideners.from(fileTree(sourceSets.main.get().resources.srcDirs.first()) {
        include("*.accesswidener")
    })
}
