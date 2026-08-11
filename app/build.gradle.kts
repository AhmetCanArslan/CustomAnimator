import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
}

val localProps = rootProject.file("local.properties").takeIf { it.exists() }?.let {
    Properties().apply { load(it.inputStream()) }
}

fun propOrEnv(name: String, default: String): String {
    return localProps?.getProperty(name) ?: System.getenv(name.replace('.', '_')) ?: default
}

val admobAppId = propOrEnv("admob.app.id", "ca-app-pub-3940256099942544~3347511713")
val admobBannerId = propOrEnv("admob.banner.id", "ca-app-pub-3940256099942544/6300978111")
val admobInterstitialId = propOrEnv("admob.interstitial.id", "ca-app-pub-3940256099942544/1033173712")
val admobAppOpenId = propOrEnv("admob.appopen.id", "ca-app-pub-3940256099942544/9257395921")

android {
    namespace = "com.arslan.customanimator"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.arslan.customanimator"
        minSdk = 24
        targetSdk = 36
        versionCode = 174
        versionName = "3.1"

        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "BANNER_AD_UNIT_ID", "\"$admobBannerId\"")
        buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"$admobInterstitialId\"")
        buildConfigField("String", "APP_OPEN_AD_UNIT_ID", "\"$admobAppOpenId\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
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
    implementation(libs.androidx.compose.material.icons.extended)
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    implementation(libs.zxing.core)
    implementation(libs.gson)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.billing.ktx)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
