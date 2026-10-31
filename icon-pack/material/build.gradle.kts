plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.kunzisoft.keepass.icon.material"
    compileSdk = 36

    defaultConfig {
        minSdk = 19
    }

    resourcePrefix = "material_"
}
