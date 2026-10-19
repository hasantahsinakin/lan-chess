plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.lanchess"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.lanchess"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation("com.github.bhlangonijr:chesslib:1.3.4")
}
