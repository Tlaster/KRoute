plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.devtools.ksp").version("1.6.10-1.0.2")
}

dependencies {
    val compose = "1.1.0-rc01"
    implementation("androidx.compose.ui:ui:$compose")
    implementation("androidx.compose.foundation:foundation:$compose")
    implementation("androidx.compose.material:material:$compose")
    implementation("androidx.activity:activity-compose:1.4.0")
    implementation("androidx.navigation:navigation-compose:2.4.0-rc01")
    implementation(project(":kroute"))
    ksp(project(":kroute"))
}

android {
    compileSdk = 31
    defaultConfig {
        applicationId = "moe.tlaster.kroute.sample"
        minSdk = 21
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.1.0-rc02"
    }
}
