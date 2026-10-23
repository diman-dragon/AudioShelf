import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val appVersion: String = rootProject.file("version.txt").readText().trim()
val versionParts = appVersion.split(".").map { it.toInt() }
require(versionParts.size == 3) { "version.txt must look like 1.0.0" }

fun secret(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }

android {
    namespace = "com.audioshelf.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.audioshelf.app"
        minSdk = 24
        targetSdk = 36
        versionCode = versionParts[0] * 10000 + versionParts[1] * 100 + versionParts[2]
        versionName = appVersion
    }

    androidResources {
        localeFilters += listOf("en", "ru")
    }

    signingConfigs {
        if (secret("AUDIOSHELF_DEBUG_KEYSTORE_FILE") != null) {
            create("audioshelfDebug") {
                storeFile = file(secret("AUDIOSHELF_DEBUG_KEYSTORE_FILE")!!)
                storePassword = secret("AUDIOSHELF_DEBUG_KEYSTORE_PASSWORD")
                keyAlias = secret("AUDIOSHELF_DEBUG_KEY_ALIAS")
                keyPassword = secret("AUDIOSHELF_DEBUG_KEY_PASSWORD")
            }
        }
        if (secret("AUDIOSHELF_KEYSTORE_FILE") != null) {
            create("audioshelfRelease") {
                storeFile = file(secret("AUDIOSHELF_KEYSTORE_FILE")!!)
                storePassword = secret("AUDIOSHELF_KEYSTORE_PASSWORD")
                keyAlias = secret("AUDIOSHELF_KEY_ALIAS")
                keyPassword = secret("AUDIOSHELF_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            signingConfigs.findByName("audioshelfDebug")?.let { signingConfig = it }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("audioshelfRelease")?.let { signingConfig = it }
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
