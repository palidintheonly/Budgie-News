plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.budgienews.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.budgienews.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.0.2-alpha"
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation("androidx.core:core-ktx:1.17.0")
    implementation(libs.compose.material3)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
