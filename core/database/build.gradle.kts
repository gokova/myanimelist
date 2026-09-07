plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.gokova.myanimelist.core.database"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
