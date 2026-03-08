import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.build.paperweight)
    implementation(libs.build.shadow)
}

dependencies {
    compileOnly(files(libs::class.java.protectionDomain.codeSource.location))
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    target {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }
}
