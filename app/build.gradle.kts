plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // NOTE: No google.services plugin — Provisioner does NOT need Firebase
}

android {
    namespace = "com.emilock.provisioner"
    compileSdk = 35

    signingConfigs {
        create("release") {
            // It's better to use a relative path or project property
            val keystoreFile = rootProject.file("KeyFileBSNLock-provisioner.jks")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
            } else {
                // Fallback to absolute path or just log a warning if needed
                storeFile = file("D:/Server/KeyFileBSNLock-provisioner")
            }
            storePassword = "sandeep500"
            keyAlias = "bsnlock"
            keyPassword = "sandeep500"
        }
    }
    defaultConfig {
        applicationId = "com.emilock.provisioner"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Coroutines (for APK download + ownership transfer)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // Gson (for QR code JSON generation)
    implementation("com.google.code.gson:gson:2.11.0")
    // ZXing QR code generator
    implementation("com.google.zxing:core:3.5.3")
}
