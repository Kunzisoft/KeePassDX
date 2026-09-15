plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.ksp) apply false
}

tasks.register<Delete>("clean") {
    description = "Clean the project"
    delete(rootProject.layout.buildDirectory)
}
