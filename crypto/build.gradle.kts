plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.kunzisoft.encrypt"
    compileSdk = 36
    ndkVersion = "25.2.9519653"

    defaultConfig {
        minSdk = 19
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        @Suppress("UnstableApiUsage")
        externalNativeBuild {
            cmake {
                // NDK <= 27
                // arguments("-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON")
                // Use manual linker flags for NDK < 27
                cppFlags("-Wl,-z,max-page-size=16384")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    externalNativeBuild {
        cmake {
            path("src/main/jni/CMakeLists.txt")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            // Bouncy castle bug https://github.com/bcgit/bc-java/issues/1685
            pickFirsts.add("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
        }
    }
}

dependencies {
    // Crypto
    implementation(libs.bouncycastle.pkix)

    androidTestImplementation(libs.androidx.test.runner)
}
