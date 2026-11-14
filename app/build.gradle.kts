plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val ksFile = System.getenv("KEYSTORE_FILE")?.let { file(it) }

android {
    namespace = "cr.andres.floatdash"
    compileSdk = 34

    defaultConfig {
        applicationId = "cr.andres.floatdash"
        minSdk = 26
        targetSdk = 29
        versionCode = 3
        versionName = "0.3"
    }

    signingConfigs {
        create("perm") {
            if (ksFile != null && ksFile.exists()) {
                storeFile = ksFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (ksFile != null && ksFile.exists()) signingConfigs.getByName("perm") else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { buildConfig = true }

    lint { abortOnError = false; checkReleaseBuilds = false }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
