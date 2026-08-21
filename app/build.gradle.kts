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
val admobRewardedId = propOrEnv("admob.rewarded.id", "ca-app-pub-3940256099942544/5224354917")

android {
    namespace = "com.arslan.customanimator"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.arslan.customanimator"
        minSdk = 24
        targetSdk = 36
        versionCode = 179
        versionName = "3.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "BANNER_AD_UNIT_ID", "\"$admobBannerId\"")
        buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"$admobInterstitialId\"")
        buildConfigField("String", "APP_OPEN_AD_UNIT_ID", "\"$admobAppOpenId\"")
        buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"$admobRewardedId\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
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
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
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
    implementation(libs.androidx.fragment.ktx)
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

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
