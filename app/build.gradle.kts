import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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

android {
    namespace = "com.arslan.customanimator"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.arslan.customanimator"
        minSdk = 24
        targetSdk = 36
        versionCode = 162
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("github") {
            dimension = "distribution"
            buildConfigField("boolean", "HAS_ADS", "false")
        }
        create("playstore") {
            dimension = "distribution"
            buildConfigField("boolean", "HAS_ADS", "true")
            manifestPlaceholders["admobAppId"] = admobAppId
            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"$admobBannerId\"")
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"$admobInterstitialId\"")
        }
    }

    buildTypes {
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
    "playstoreImplementation"(libs.play.services.ads)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}