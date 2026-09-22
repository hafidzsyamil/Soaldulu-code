import java.util.Properties

// Alamat repo dan token GitHub dibaca dari local.properties, yang tidak ikut
// masuk git. Kalau kosong, fitur kirim laporan mati dan aplikasi tetap jalan
// seperti biasa.
val rahasia = Properties().apply {
    val berkas = rootProject.file("local.properties")
    if (berkas.exists()) berkas.inputStream().use { load(it) }
}

fun rahasiaKutip(kunci: String, bawaan: String = ""): String =
    "\"" + (rahasia.getProperty(kunci) ?: bawaan).trim() + "\""

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "id.soaldulu.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "id.soaldulu.app"
        minSdk = 29
        // Sengaja 34, bukan 36: aturan foreground service Android 15+
        // (batas waktu FGS harian) tidak berlaku untuk targetSdk 34.
        // Lihat handoff Bagian 3.1 dan rencana Fase 0.
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "GITHUB_REPO", rahasiaKutip("soaldulu.github.repo"))
        buildConfigField("String", "GITHUB_TOKEN", rahasiaKutip("soaldulu.github.token"))
        buildConfigField("String", "GITHUB_BRANCH", rahasiaKutip("soaldulu.github.branch", "main"))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Ikon yang dipakai: Person, Edit, Check, Close, Settings, ArrowBack, Add, Delete.
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    debugImplementation(libs.androidx.compose.ui.tooling)
}