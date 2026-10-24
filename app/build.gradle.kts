import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val versionProps = Properties().apply {
    val propFile = rootProject.file("version.properties")
    if (propFile.exists()) {
        FileInputStream(propFile).use { load(it) }
    }
}

val appVersionCode = versionProps.getProperty("VERSION_BUILD", "20002").toInt()
val appVersionName = versionProps.getProperty("VERSION_NAME", "2.0.0-rc2")
val appChannel = versionProps.getProperty("VERSION_CHANNEL", "rc")
val gitSha = versionProps.getProperty("GIT_COMMIT_SHA", "6c0b403")
val buildTimestamp = versionProps.getProperty("BUILD_TIME", "2026-10-24T21:40:00Z")

android {
    namespace = "com.progalleryx.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.progalleryx.app"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GIT_COMMIT_SHA", "\"$gitSha\"")
        buildConfigField("String", "BUILD_TIME", "\"$buildTimestamp\"")
        buildConfigField("String", "RELEASE_CHANNEL", "\"$appChannel\"")
        buildConfigField("String", "EXPECTED_SIGNING_SHA256", "\"c1ea4285c43ccc6da990b93e9a323494609aaa68dbe2d1f500c24c3d6956c1f6\"")
    }

    signingConfigs {
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

afterEvaluate {
    tasks.findByName("assembleDebug")?.doLast {
        val src = file("build/outputs/apk/debug/app-debug.apk")
        val dstDir = file("build/outputs/apk/distribution")
        val dst = file("build/outputs/apk/distribution/ProGalleryX-$appVersionName-$appVersionCode-$gitSha-debug.apk")
        if (src.exists()) {
            dstDir.mkdirs()
            src.copyTo(dst, overwrite = true)
            println("DISTRIBUTION_APK_CREATED: ${dst.absolutePath} (${dst.length()} bytes)")
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.media3:media3-transformer:1.11.1")
    implementation("androidx.media3:media3-effect:1.11.1")
    implementation("androidx.media3:media3-common:1.11.1")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-ui:1.11.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
}
