import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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
        // Date-based versions matching the ha4o-YYYYMMDD[-HHmm] release tags. CI passes
        // APP_VERSION_CODE / APP_VERSION_NAME on tag builds; local builds use today's date.
        versionCode = (System.getenv("APP_VERSION_CODE") ?: defaultVersionCode()).toInt()
        versionName = System.getenv("APP_VERSION_NAME") ?: defaultVersionName()
    }

    signingConfigs {
        getByName("debug") {
            // Committed debug keystore so every CI release signs with the same key and
            // direct-install upgrades work (same approach as R1HA).
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }

    lint {
        // HA4O ships outside Google Play and deliberately targets old Android, so the
        // Play-minimum-targetSdk check is a false positive. Everything else stays on —
        // notably NewApi, which is what guards against calling an API newer than 2.3.
        disable += "ExpiredTargetSdkVersion"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            // R8 + resource shrinking for the smallest possible APK. okhttp ships its
            // own consumer rules in the jar; proguard-rules.pro adds the standard
            // -dontwarn for okhttp/okio's optional (absent) TLS providers.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

/** Local-dev versionCode: 100M floor + minutes since 2020-01-01 UTC (strictly monotonic). */
fun defaultVersionCode(): String {
    val epoch = LocalDateTime.of(2020, 1, 1, 0, 0)
    val minutes = Duration.between(epoch, LocalDateTime.now(ZoneOffset.UTC)).toMinutes()
    return (100_000_000L + minutes).coerceAtLeast(1).toString()
}

/** Local-dev versionName: YYYY.MM.DD.HHmm in UTC. */
fun defaultVersionName(): String =
    LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy.MM.dd.HHmm"))
