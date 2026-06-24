plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.ucar.launcher"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.ucar.launcher"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
}
