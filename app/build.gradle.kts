plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.redstars.artvault"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.redstars.artvault"
        minSdk = 28
        targetSdk = 35
        versionCode = 3
        versionName = "1.2"
    }

    // 고정 서명 키: 빌드할 때마다 같은 키로 서명해야 덮어 설치(업데이트)가 되고 작품이 유지됨
    signingConfigs {
        create("artvault") {
            storeFile = file("artvault.jks")
            storePassword = "artvault123"
            keyAlias = "artvault"
            keyPassword = "artvault123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("artvault")
        }
        debug {
            signingConfig = signingConfigs.getByName("artvault")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("io.coil-kt:coil-compose:2.7.0")
}
