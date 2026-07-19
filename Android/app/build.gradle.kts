plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.oucai.llama"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.oucai.llama.aichat"

        minSdk = 26
        targetSdk = 36

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Hardcode signature constants into the generated BuildConfig wrapper class
        buildConfigField("String", "DEVELOPER_SIGNATURE", "\"Made by Zalan Lykos\"")
        buildConfigField("String", "DEVELOPER_WEBSITE", "\"https://zalanlykos.github.io\"")
        buildConfigField("String", "LICENSE_TYPE", "\"OpenSource-Copyleft-GPL3\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    implementation(libs.bundles.androidx)
    implementation(libs.material)

    implementation(project(":lib"))

    implementation(libs.okhttp)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
