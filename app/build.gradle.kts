import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val properties = Properties()
val localPropertiesFile = project.rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    properties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.example.camerafunction"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.camerafunction"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        forEach { buildType ->
            val apiKey = properties.getProperty("GEMINI_API_KEY")
            val rfKey = properties.getProperty("RF_API")
            buildType.buildConfigField("String", "GEMINI_API_KEY", "\"$apiKey\"")
            buildType.buildConfigField("String", "RF_API", "\"$rfKey\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // ===================================================================
    // TAMBAHKAN BLOK INI UNTUK MENGAKTIFKAN KEMBALI BUILDCONFIG
    // ===================================================================
    buildFeatures {
        buildConfig = true
    }
    // ===================================================================
    // AKHIR DARI BLOK YANG DITAMBAHKAN
    // ===================================================================
}

dependencies {
    // ... dependensi Anda ...
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.generativeai)
    implementation(libs.guava)
    implementation(libs.bundles.camera)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.glide)
    implementation(platform(libs.okhttp.bom)) // Use the BOM for consistent OkHttp versions
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor) // Optional: for network request logging
    implementation(libs.gson) // For JSON parsing
    implementation(libs.photoview)
    implementation(libs.markwon.core)
    implementation(libs.markwon.linkify)

}