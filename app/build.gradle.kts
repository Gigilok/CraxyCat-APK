plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.crazycat.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.crazycat.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "3.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Configuração do NDK para o motor Keeloq em C++
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    
    // Integração do C++ 
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Rede Assíncrona (Comunicação com ESP32)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Coroutines (Para não travar a tela do celular)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}
