import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
}

val hasGoogleServices = file("google-services.json").exists()
if (hasGoogleServices) {
    pluginManager.apply(
        libs.plugins.google.services
            .get()
            .pluginId,
    )
    pluginManager.apply(
        libs.plugins.firebase.crashlytics
            .get()
            .pluginId,
    )
    pluginManager.apply(
        libs.plugins.firebase.appdistribution
            .get()
            .pluginId,
    )
} else {
    logger.warn("google-services.json not found in app/. Firebase plugins will not be applied.")
}

val localProperties =
    Properties().apply {
        val localPropFile = rootDir.resolve("local.properties")
        if (localPropFile.exists()) {
            localPropFile.inputStream().use { load(it) }
        }
    }

val releaseKeystorePath: String? =
    localProperties.getProperty("RELEASE_KEYSTORE_PATH")
        ?: System.getenv("RELEASE_KEYSTORE_PATH")
val releaseKeystorePassword: String? =
    localProperties.getProperty("RELEASE_KEYSTORE_PASSWORD")
        ?: System.getenv("RELEASE_KEYSTORE_PASSWORD")
val releaseKeyAlias: String? =
    localProperties.getProperty("RELEASE_KEY_ALIAS")
        ?: System.getenv("RELEASE_KEY_ALIAS")
val releaseKeyPassword: String? =
    localProperties.getProperty("RELEASE_KEY_PASSWORD")
        ?: System.getenv("RELEASE_KEY_PASSWORD")

android {
    namespace = "com.gokova.myanimelist"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.gokova.myanimelist"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystoreFile =
                releaseKeystorePath?.let { path ->
                    val direct = file(path)
                    if (direct.exists()) direct else rootDir.resolve(path)
                }
            if (keystoreFile != null && keystoreFile.exists() && releaseKeystorePassword != null) {
                storeFile = keystoreFile
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            } else {
                // Fallback to debug keystore for open-source contributors
                val debugSigning = getByName("debug")
                storeFile = debugSigning.storeFile
                storePassword = debugSigning.storePassword
                keyAlias = debugSigning.keyAlias
                keyPassword = debugSigning.keyPassword
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = false
            }
            if (hasGoogleServices) {
                firebaseAppDistribution {
                    artifactType = "APK"
                    val releaseNotesText: String =
                        localProperties.getProperty("FIREBASE_APP_DISTRIBUTION_RELEASE_NOTES")
                            ?: System.getenv("FIREBASE_APP_DISTRIBUTION_RELEASE_NOTES")
                            ?: "MyAnimeList release build"
                    releaseNotes = releaseNotesText

                    val credProp: String? =
                        localProperties.getProperty("FIREBASE_APP_DISTRIBUTION_CREDENTIALS")
                            ?: System.getenv("FIREBASE_APP_DISTRIBUTION_CREDENTIALS")
                    val credFile: String? =
                        credProp?.let { path ->
                            val direct = file(path)
                            if (direct.exists()) {
                                direct.absolutePath
                            } else {
                                rootDir.resolve(path).takeIf { it.exists() }?.absolutePath
                            }
                        } ?: rootDir
                            .resolve("service-account.json")
                            .takeIf { it.exists() }
                            ?.absolutePath
                    if (credFile != null) {
                        serviceCredentialsFile = credFile
                    }

                    val groupList: String =
                        localProperties.getProperty("FIREBASE_APP_DISTRIBUTION_GROUPS")
                            ?: System.getenv("FIREBASE_APP_DISTRIBUTION_GROUPS")
                            ?: "internal"
                    groups = groupList
                }
            }
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
    // Project Modules
    implementation(project(":core:domain"))
    implementation(project(":core:datastore"))
    implementation(project(":core:network"))
    implementation(project(":core:ui"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:mylist"))
    implementation(project(":feature:taste"))
    implementation(project(":feature:recommendation"))

    // Logging
    implementation(libs.timber)

    // Firebase (Crashlytics)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)

    // Platform Dependencies
    implementation(platform(libs.androidx.compose.bom))

    // AndroidX & Core
    implementation(libs.androidx.browser)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Jetpack Compose UI
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation & Serialization
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Dependency Injection (Hilt)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    // Background Processing
    implementation(libs.androidx.work.runtime.ktx)

    // Debugging Tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Android Instrumentation Testing
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
