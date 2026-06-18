plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.github.itskenny0.ha4o"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.github.itskenny0.ha4o"
        // Android 2.3 Gingerbread. The whole point of HA4O.
        minSdk = 9
        // Low targetSdk so newer Android runs the app in legacy-compat mode rather
        // than applying runtime-permission / background restrictions this UI can't handle.
        targetSdk = 10
        versionCode = 1
        versionName = "0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    // okhttp 3.12.x is the last branch that supports Android 2.3 (API 9) and Java 7.
    // Frozen for security since 2018, which is acceptable here: HA4O only ever speaks
    // plain HTTP/ws on the local network (Gingerbread can't negotiate modern TLS anyway).
    implementation("com.squareup.okhttp3:okhttp:3.12.13")

    testImplementation("junit:junit:4.13.2")
    // The real org.json implementation so JSON-parsing unit tests run on the JVM
    // (the android.jar org.json is a stub that throws under plain unit tests).
    testImplementation("org.json:json:20240303")
}
