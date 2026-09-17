import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
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
val admobRewardedId = propOrEnv("admob.rewarded.id", "ca-app-pub-3940256099942544/5224354917")

val admobTestPublisherPrefix = "ca-app-pub-3940256099942544"

val verifyReleaseAdUnitIds = tasks.register("verifyReleaseAdUnitIds") {
    val configuredIds = listOf(
        "admob.app.id" to admobAppId,
        "admob.banner.id" to admobBannerId,
        "admob.interstitial.id" to admobInterstitialId,
        "admob.rewarded.id" to admobRewardedId
    )
    doLast {
        val missing = configuredIds.filter { it.second.startsWith(admobTestPublisherPrefix) }
        if (missing.isNotEmpty()) {
            throw GradleException(
                "Release build would ship AdMob test ad units. Set " +
                    missing.joinToString(", ") { it.first } +
                    " in local.properties or the matching environment variables."
            )
        }
    }
}

tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
    dependsOn(verifyReleaseAdUnitIds)
}

android {
    namespace = "com.arslan.customanimator"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.arslan.customanimator"
        minSdk = 24
        targetSdk = 36
        versionCode = 189
        versionName = "3.6"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "BANNER_AD_UNIT_ID", "\"$admobBannerId\"")
        buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"$admobInterstitialId\"")
        buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"$admobRewardedId\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
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
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.zxing.core)
    implementation(libs.gson)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
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
