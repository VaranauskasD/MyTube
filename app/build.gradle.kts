import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Semantic versioning (https://semver.org). versionCode is derived so it stays
// strictly increasing as long as minor/patch each stay below 100.
val versionMajor = 1
val versionMinor = 0
val versionPatch = 0

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) load(FileInputStream(keystorePropertiesFile))
}
val hasReleaseSigning = keystorePropertiesFile.exists() || System.getenv("RELEASE_KEYSTORE_PATH") != null

fun releaseSigningValue(propertyKey: String, envVar: String): String? =
    keystoreProperties.getProperty(propertyKey) ?: System.getenv(envVar)

android {
    namespace = "com.varanauskasd.mytube"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.varanauskasd.mytube"
        minSdk = 26
        targetSdk = 35
        versionCode = versionMajor * 10_000 + versionMinor * 100 + versionPatch
        versionName = "$versionMajor.$versionMinor.$versionPatch"
    }

    signingConfigs {
        create("release") {
            releaseSigningValue("storeFile", "RELEASE_KEYSTORE_PATH")?.let { storeFile = rootProject.file(it) }
            storePassword = releaseSigningValue("storePassword", "RELEASE_KEYSTORE_PASSWORD")
            keyAlias = releaseSigningValue("keyAlias", "RELEASE_KEY_ALIAS")
            keyPassword = releaseSigningValue("keyPassword", "RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Falls back to debug signing when no release keystore is configured
            // (e.g. a fresh clone without keystore.properties or CI secrets),
            // so `assembleRelease` always produces an installable APK.
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
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
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    // Pure-Kotlin/Java YouTube scraper — no API key, no account.
    implementation("com.github.teamnewpipe:NewPipeExtractor:v0.26.5")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")
}
